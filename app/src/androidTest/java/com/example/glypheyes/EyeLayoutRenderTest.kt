package com.example.glypheyes

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.roundToInt

/**
 * EyeLayoutのプリセット(Phone 3: 25×25 / 4a Pro: 13×13)が
 * EyeRendererで期待どおり描画できるかを検証し、PNGを書き出す。
 *
 * 出力先: /sdcard/Android/data/com.example.glypheyes/files/render_check/
 * 取り出し: adb pull /sdcard/Android/data/com.example.glypheyes/files/render_check/
 */
@RunWith(AndroidJUnit4::class)
class EyeLayoutRenderTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun renderAndSave(layout: EyeLayout, name: String, block: RenderArgs.() -> Unit) {
        val renderer = EyeRenderer(layout)
        val args = RenderArgs(layout, renderer).apply(block)
        val bm = renderer.drawEyes(
            leftEyeCenter = args.leftCenter,
            rightEyeCenter = args.rightCenter,
            eyeRadiusX = args.rx,
            eyeRadiusY = args.ry,
            pupilLeftCenter = args.pupilLeft,
            pupilRightCenter = args.pupilRight,
            pupilRadius = layout.pupilRadius,
            blink = args.blink,
            sleep = args.sleep,
            angry = args.angry,
            squint = args.squint,
            wink = args.wink
        )
        assertEquals(layout.size, bm.width)
        assertEquals(layout.size, bm.height)

        // 拡大PNG(最近傍補間でドット感を維持)を保存
        val scale = 400 / layout.size
        val enlarged = Bitmap.createScaledBitmap(bm, layout.size * scale, layout.size * scale, false)
        val dir = File(context.getExternalFilesDir(null), "render_check").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { enlarged.compress(Bitmap.CompressFormat.PNG, 100, it) }

        // 輝度閾値で点灯ピクセルを数える
        // アンチエイリアスでグレーになったピクセルもGlyph Matrixは輝度として描画するため
        // 閾値は50(薄暗い)とし、純白を要求しない
        // ※ blink=1(完全に閉じた目)は全消灯が正しいのでチェックしない
        if (args.blink < 1f) {
            val lit = countLit(bm)
            assertTrue("$name: no lit pixels\n${asciiArt(bm)}", lit > 0)
        }
    }

    private fun countLit(bm: Bitmap): Int {
        var count = 0
        for (y in 0 until bm.height) for (x in 0 until bm.width) {
            if (Color.red(bm.getPixel(x, y)) > 50) count++
        }
        return count
    }

    /** デバッグ用ASCIIアート出力(# >200, + >50, . それ以外) */
    private fun asciiArt(bm: Bitmap): String = buildString {
        for (y in 0 until bm.height) {
            for (x in 0 until bm.width) {
                append(when (Color.red(bm.getPixel(x, y))) {
                    in 201..255 -> '#'
                    in 51..200 -> '+'
                    else -> '.'
                })
            }
            append('\n')
        }
    }

    @Test
    fun renderPhone3Frames() = renderFramesFor(EyeLayout.PHONE_3, "phone3")

    @Test
    fun renderPhone4aProFrames() = renderFramesFor(EyeLayout.PHONE_4A_PRO, "phone4apro")

    private fun renderFramesFor(layout: EyeLayout, prefix: String) {
        val c by lazy { layout.leftCenter }

        renderAndSave(layout, "${prefix}_01_center") {
            pupilLeft = c; pupilRight = rightCenter
        }
        renderAndSave(layout, "${prefix}_02_look_right") {
            pupilLeft = shifted(layout.limitRight, 0); pupilRight = shifted(layout.limitRight, 0)
        }
        renderAndSave(layout, "${prefix}_03_look_up") {
            pupilLeft = shifted(0, layout.limitUp); pupilRight = shifted(0, layout.limitUp)
        }
        renderAndSave(layout, "${prefix}_04_blink_mid") {
            pupilLeft = c; pupilRight = rightCenter; blink = 0.5f
        }
        renderAndSave(layout, "${prefix}_05_blink_full") {
            pupilLeft = c; pupilRight = rightCenter; blink = 1f
        }
        renderAndSave(layout, "${prefix}_06_wink") {
            pupilLeft = c; pupilRight = rightCenter; wink = 1f
        }
        renderAndSave(layout, "${prefix}_07_squint") {
            pupilLeft = c; pupilRight = rightCenter; squint = 1f
        }
        renderAndSave(layout, "${prefix}_08_sleep") {
            pupilLeft = c; pupilRight = rightCenter; sleep = 1f
        }
        renderAndSave(layout, "${prefix}_09_angry") {
            pupilLeft = c; pupilRight = rightCenter; angry = 1f
        }
        renderAndSave(layout, "${prefix}_10_surprise") {
            // サービスと同じ驚きロジック: 1.3倍(小さいレイアウトは丸めで消えるため最低+1px)
            val boost = if (layout.radiusX <= 2) 1 else (layout.radiusX * 0.3f).roundToInt()
            rx = layout.radiusX + boost; ry = layout.radiusY + boost
            pupilLeft = c; pupilRight = rightCenter
        }
    }

    /** drawEyesへ渡す引数ビルダー */
    private class RenderArgs(
        val layout: EyeLayout,
        val renderer: EyeRenderer
    ) {
        var rx = layout.radiusX
        var ry = layout.radiusY
        var pupilLeft = layout.leftCenter
        var pupilRight = layout.rightCenter
        var blink = 0f
        var sleep = 0f
        var angry = 0f
        var squint = 0f
        var wink = 0f

        val leftCenter get() = layout.leftCenter
        val rightCenter get() = layout.rightCenter

        /** clampを通して瞳位置をずらす(サービスと同じ経路) */
        fun shifted(dx: Int, dy: Int): Pair<Int, Int> = renderer.clamp(
            leftCenter,
            leftCenter.first + dx to leftCenter.second + dy,
            rx, layout.pupilRadius
        )
    }
}
