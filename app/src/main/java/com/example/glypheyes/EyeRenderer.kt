package com.example.glypheyes

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.*

/**
 * 目のビットマップを描画するクラス
 * Paint オブジェクトと Bitmap を再利用してメモリ効率を向上
 */
class EyeRenderer {
    
    // 再利用可能な Bitmap と Canvas
    private val reusableBitmap = Bitmap.createBitmap(
        EyeConstants.BITMAP_WIDTH, 
        EyeConstants.BITMAP_HEIGHT, 
        Bitmap.Config.ARGB_8888
    )
    private val canvas = Canvas(reusableBitmap)
    
    // 再利用可能な Paint オブジェクト
    private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = EyeConstants.EYE_OUTLINE_WIDTH.toFloat()
    }
    
    private val blackFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    
    private val highlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    
    private val angryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    
    /**
     * 目のビットマップを描画
     */
    fun drawEyes(
        leftEyeCenter: Pair<Int, Int>,
        rightEyeCenter: Pair<Int, Int>,
        eyeRadiusX: Int,
        eyeRadiusY: Int,
        pupilLeftCenter: Pair<Int, Int>,
        pupilRightCenter: Pair<Int, Int>,
        pupilRadius: Int,
        blink: Float,
        sleep: Float = 0f,
        angry: Float = 0f,
        squint: Float = 0f
    ): Bitmap {
        // 背景をクリア（黒 = 消灯）
        canvas.drawColor(Color.BLACK)
        
        // 接続した縦楕円の白目を描画
        drawConnectedEyes(leftEyeCenter, rightEyeCenter, eyeRadiusX, eyeRadiusY)
        
        // 黒縁を描画
        drawOutline(leftEyeCenter, rightEyeCenter, eyeRadiusX, eyeRadiusY)
        
        // 瞳とハイライトを描画
        drawPupilAndHighlight(pupilLeftCenter, pupilRadius)
        drawPupilAndHighlight(pupilRightCenter, pupilRadius)
        
        // まばたき（上下から黒バー）
        if (blink > 0f) {
            val cover = (eyeRadiusY * blink).roundToInt().coerceAtLeast(1)
            val topY = max(0, leftEyeCenter.second - eyeRadiusY)
            val botY = min(EyeConstants.BITMAP_HEIGHT, leftEyeCenter.second + eyeRadiusY)
            canvas.drawRect(0f, topY.toFloat(), EyeConstants.BITMAP_WIDTH.toFloat(), (topY + cover).toFloat(), blackFill)
            canvas.drawRect(0f, (botY - cover).toFloat(), EyeConstants.BITMAP_WIDTH.toFloat(), botY.toFloat(), blackFill)
        }
        
        // 眠気（上まぶただけが降りる）
        if (sleep > 0f) {
            val sleepCover = (eyeRadiusY * sleep * 0.7f).roundToInt().coerceAtLeast(1)
            val topY = max(0, leftEyeCenter.second - eyeRadiusY)
            canvas.drawRect(0f, topY.toFloat(), EyeConstants.BITMAP_WIDTH.toFloat(), (topY + sleepCover).toFloat(), blackFill)
        }
        
        // 怒り（眉毛）
        if (angry > 0f) {
            val leftEyebrowY = leftEyeCenter.second - eyeRadiusY - 2
            canvas.drawLine(
                (leftEyeCenter.first - eyeRadiusX + 1).toFloat(),
                leftEyebrowY.toFloat(),
                (leftEyeCenter.first + eyeRadiusX - 1).toFloat(),
                (leftEyebrowY - 2 * angry).toFloat(),
                angryPaint
            )
            val rightEyebrowY = rightEyeCenter.second - eyeRadiusY - 2
            canvas.drawLine(
                (rightEyeCenter.first - eyeRadiusX + 1).toFloat(),
                (rightEyebrowY - 2 * angry).toFloat(),
                (rightEyeCenter.first + eyeRadiusX - 1).toFloat(),
                rightEyebrowY.toFloat(),
                angryPaint
            )
        }
        
        // 目が笑う状態（目を細くする）
        if (squint > 0f) {
            val squintCover = (eyeRadiusY * squint * 0.8f).roundToInt().coerceAtLeast(1)
            val topY = max(0, leftEyeCenter.second - eyeRadiusY)
            val botY = min(EyeConstants.BITMAP_HEIGHT, leftEyeCenter.second + eyeRadiusY)
            canvas.drawRect(0f, topY.toFloat(), EyeConstants.BITMAP_WIDTH.toFloat(), (topY + squintCover).toFloat(), blackFill)
            canvas.drawRect(0f, (botY - squintCover).toFloat(), EyeConstants.BITMAP_WIDTH.toFloat(), botY.toFloat(), blackFill)
        }
        
        return reusableBitmap
    }
    
    /**
     * 接続した縦楕円の白目を描画
     */
    private fun drawConnectedEyes(
        leftEyeCenter: Pair<Int, Int>,
        rightEyeCenter: Pair<Int, Int>,
        eyeRadiusX: Int,
        eyeRadiusY: Int
    ) {
        // 左目（縦楕円）
        canvas.drawOval(
            (leftEyeCenter.first - eyeRadiusX).toFloat(),
            (leftEyeCenter.second - eyeRadiusY).toFloat(),
            (leftEyeCenter.first + eyeRadiusX).toFloat(),
            (leftEyeCenter.second + eyeRadiusY).toFloat(),
            whitePaint
        )
        // 右目（縦楕円）
        canvas.drawOval(
            (rightEyeCenter.first - eyeRadiusX).toFloat(),
            (rightEyeCenter.second - eyeRadiusY).toFloat(),
            (rightEyeCenter.first + eyeRadiusX).toFloat(),
            (rightEyeCenter.second + eyeRadiusY).toFloat(),
            whitePaint
        )
        // 中央の接続部分（矩形で繋ぐ）
        val centerY = leftEyeCenter.second.toFloat()
        val connectionWidth = (rightEyeCenter.first - leftEyeCenter.first - eyeRadiusX * 2).toFloat()
        if (connectionWidth > 0) {
            canvas.drawRect(
                (leftEyeCenter.first + eyeRadiusX).toFloat(),
                (centerY - eyeRadiusY / 2f),
                (rightEyeCenter.first - eyeRadiusX).toFloat(),
                (centerY + eyeRadiusY / 2f),
                whitePaint
            )
        }
    }
    
    /**
     * 黒縁を描画
     */
    private fun drawOutline(
        leftEyeCenter: Pair<Int, Int>,
        rightEyeCenter: Pair<Int, Int>,
        eyeRadiusX: Int,
        eyeRadiusY: Int
    ) {
        // 左目の縁（縦楕円）
        canvas.drawOval(
            (leftEyeCenter.first - eyeRadiusX).toFloat(),
            (leftEyeCenter.second - eyeRadiusY).toFloat(),
            (leftEyeCenter.first + eyeRadiusX).toFloat(),
            (leftEyeCenter.second + eyeRadiusY).toFloat(),
            strokePaint
        )
        // 右目の縁（縦楕円）
        canvas.drawOval(
            (rightEyeCenter.first - eyeRadiusX).toFloat(),
            (rightEyeCenter.second - eyeRadiusY).toFloat(),
            (rightEyeCenter.first + eyeRadiusX).toFloat(),
            (rightEyeCenter.second + eyeRadiusY).toFloat(),
            strokePaint
        )
        // 接続部分の縁
        val centerY = leftEyeCenter.second.toFloat()
        val connectionWidth = (rightEyeCenter.first - leftEyeCenter.first - eyeRadiusX * 2).toFloat()
        if (connectionWidth > 0) {
            val strokeRect = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = EyeConstants.EYE_OUTLINE_WIDTH.toFloat()
            }
            canvas.drawRect(
                (leftEyeCenter.first + eyeRadiusX).toFloat(),
                (centerY - eyeRadiusY / 2f),
                (rightEyeCenter.first - eyeRadiusX).toFloat(),
                (centerY + eyeRadiusY / 2f),
                strokeRect
            )
        }
    }
    
    /**
     * 瞳とハイライトを描画（瞳は左下寄り、ハイライトは細長い三日月形）
     */
    private fun drawPupilAndHighlight(pupilCenter: Pair<Int, Int>, pupilRadius: Int) {
        // 瞳（左下寄りに配置）
        val pupilOffsetX = -pupilRadius / 2 // 左寄り
        val pupilOffsetY = pupilRadius / 2  // 下寄り
        val finalPupilX = pupilCenter.first + pupilOffsetX
        val finalPupilY = pupilCenter.second + pupilOffsetY
        canvas.drawCircle(finalPupilX.toFloat(), finalPupilY.toFloat(), pupilRadius.toFloat(), blackFill)
        
        // 細長い三日月形のハイライト（瞳の右上端）
        val hlX = finalPupilX + pupilRadius * 0.6f
        val hlY = finalPupilY - pupilRadius * 0.6f
        val hlWidth = pupilRadius * 1.2f  // より細長く
        val hlHeight = pupilRadius * 0.2f // より薄く
        
        // 細長い楕円形のハイライト
        canvas.save()
        canvas.translate(hlX, hlY)
        canvas.rotate(25f) // 右斜め上向き
        canvas.drawOval(-hlWidth/2f, -hlHeight/2f, hlWidth/2f, hlHeight/2f, highlight)
        canvas.restore()
    }
    /**
     * 瞳のはみ出し防止(楕円内にクランプ・上下で許容量を変える)
     */
    fun clamp(center: Pair<Int, Int>, target: Pair<Int, Int>, eyeRadiusX: Int, pupilRadius: Int): Pair<Int, Int> {
        val dx = (target.first - center.first).toFloat()
        val dy = (target.second - center.second).toFloat()
        
        // 横は瞳半径ぶんだけ内側に。縦は上方向(+2px猶予)、下方向(-1px厳しめ)
        val safeRadiusYUp = (eyeRadiusX - pupilRadius + 2).coerceAtLeast(1)    // 上に+2px伸ばす
        val safeRadiusYDown = (eyeRadiusX - pupilRadius - 1).coerceAtLeast(1)   // 下はやや厳しめ
        
        val ry = if (dy < 0f) safeRadiusYUp.toFloat() else safeRadiusYDown.toFloat()
        val safeRadiusXRight = (eyeRadiusX - pupilRadius + 1).coerceAtLeast(1)
        // 左方向は黒縁に潜り込まないように1px手前で止める
        val safeRadiusXLeft = (eyeRadiusX - pupilRadius - 1).coerceAtLeast(1)
        val rx = (if (dx > 0f) safeRadiusXRight else safeRadiusXLeft).toFloat()
        
        val ellipseDist = (dx * dx) / (rx * rx) + (dy * dy) / (ry * ry)
        if (ellipseDist <= 1f) {
            return target
        }
        val scale = 1f / sqrt(ellipseDist)
        val clampedX = center.first + (dx * scale).roundToInt()
        val clampedY = center.second + (dy * scale).roundToInt()
        return Pair(clampedX, clampedY)
    }
}
