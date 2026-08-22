package com.example.glypheyes

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import kotlin.reflect.KMutableProperty0

/**
 * 目の表情アニメーションを制御するクラス
 * まばたき、驚き、眠気、怒り、笑顔、ウインクなどのアニメーションを管理
 */
class AnimationController(private val eyeState: EyeState) {

    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "AnimationController"

        /** 既定カーブ: 0→1→0 の三角波（スムーズに閉じて開く） */
        private val TRIANGLE: (Float) -> Float = { t ->
            if (t < 0.5f) t * 2f else (1f - (t - 0.5f) * 2f)
        }

        /** 眠気カーブ: 最初の30%で閉じ、そのまま保つ */
        private val SLEEPY_CURVE: (Float) -> Float = { t ->
            if (t < 0.3f) t / 0.3f else 1f
        }

        /** ウインクカーブ: 30%で閉じ、40%保ち、30%で開く */
        private val WINK_CURVE: (Float) -> Float = { t ->
            when {
                t < 0.3f -> t / 0.3f
                t < 0.7f -> 1f
                else -> (1f - (t - 0.7f) / 0.3f)
            }
        }
    }

    /** まばたきをトリガー */
    fun triggerBlink() = triggerExpression(
        eyeState::isBlinking, eyeState::blinkProgress,
        EyeConstants.BLINK_DURATION_MS, TRIANGLE
    )

    /** 驚きの表情をトリガー */
    fun triggerSurprise() = triggerExpression(
        eyeState::isSurprised, eyeState::surpriseProgress,
        EyeConstants.SURPRISE_DURATION_MS, TRIANGLE
    )

    /** 眠そうな表情をトリガー */
    fun triggerSleepy() = triggerExpression(
        eyeState::isSleepy, eyeState::sleepProgress,
        EyeConstants.SLEEPY_DURATION_MS, SLEEPY_CURVE
    )

    /** 怒りの表情をトリガー */
    fun triggerAngry() = triggerExpression(
        eyeState::isAngry, eyeState::angryProgress,
        EyeConstants.ANGRY_DURATION_MS, TRIANGLE
    )

    /** 目を細める（笑顔）をトリガー */
    fun triggerSquint() = triggerExpression(
        eyeState::isSquinting, eyeState::squintProgress,
        EyeConstants.SQUINT_DURATION_MS, TRIANGLE
    )

    /** ウインク（左目だけ閉じる）をトリガー */
    fun triggerWink() = triggerExpression(
        eyeState::isWinking, eyeState::winkProgress,
        EyeConstants.WINK_DURATION_MS, WINK_CURVE
    )

    /** 充電開始時: 驚き → 少し遅れて笑顔 */
    fun triggerWakeupSmile() {
        triggerSurprise()
        mainHandler.postDelayed(
            { triggerSquint() },
            EyeConstants.WAKEUP_SMILE_DELAY_MS
        )
    }

    /** 2回まばたきしてからモードを切り替え */
    fun triggerDoubleBlinkThenSwitch(targetMode: EyeState.EyeMode, onModeChanged: () -> Unit) {
        triggerBlink()
        mainHandler.postDelayed({ triggerBlink() }, EyeConstants.DOUBLE_BLINK_INTERVAL_MS)
        mainHandler.postDelayed({
            eyeState.mode = targetMode
            onModeChanged()
        }, EyeConstants.MODE_SWITCH_DELAY_MS)
    }

    /**
     * ワンショット表情アニメーションの共通実装
     * @param running 実行中フラグ（再入防止）
     * @param progress 進行度（0.0〜1.0）の書き込み先
     * @param durationMs アニメーション時間（ミリ秒）
     * @param curve 進行度t(0..1)を表情強度へ写像するカーブ
     */
    private fun triggerExpression(
        running: KMutableProperty0<Boolean>,
        progress: KMutableProperty0<Float>,
        durationMs: Long,
        curve: (Float) -> Float
    ) {
        if (running.get()) return
        running.set(true)
        progress.set(0f)
        animate(
            durationMs,
            onUpdate = { t -> progress.set(curve(t)) },
            onEnd = {
                running.set(false)
                progress.set(0f)
            }
        )
    }

    /**
     * アニメーションを実行する汎用関数
     * @param durationMs アニメーション時間（ミリ秒）
     * @param onUpdate 進行度（0.0〜1.0）ごとに呼ばれるコールバック
     * @param onEnd アニメーション完了時のコールバック
     */
    private fun animate(durationMs: Long, onUpdate: (Float) -> Unit, onEnd: () -> Unit) {
        val start = SystemClock.uptimeMillis()
        fun step() {
            val t = ((SystemClock.uptimeMillis() - start).toFloat() / durationMs).coerceIn(0f, 1f)
            onUpdate(t)
            if (t < 1f) {
                mainHandler.postDelayed({ step() }, EyeConstants.ANIMATION_FRAME_INTERVAL_MS)
            } else {
                onEnd()
            }
        }
        step()
    }
}
