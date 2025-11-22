package com.example.glypheyes

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log

/**
 * 目の表情アニメーションを制御するクラス
 * まばたき、驚き、眠気、怒り、笑顔などのアニメーションを管理
 */
class AnimationController(private val eyeState: EyeState) {
    
    private val mainHandler = Handler(Looper.getMainLooper())
    
    companion object {
        private const val TAG = "AnimationController"
    }
    
    /**
     * まばたきをトリガー
     */
    fun triggerBlink() {
        if (eyeState.isBlinking) return
        eyeState.isBlinking = true
        eyeState.blinkProgress = 0f
        Log.d(TAG, "Blink triggered")
        animate(
            EyeConstants.BLINK_DURATION_MS,
            onUpdate = { t ->
                eyeState.blinkProgress = if (t < 0.5f) t * 2f else (1f - (t - 0.5f) * 2f)
            },
            onEnd = {
                eyeState.isBlinking = false
                eyeState.blinkProgress = 0f
            }
        )
    }
    
    /**
     * 驚きの表情をトリガー
     */
    fun triggerSurprise() {
        if (eyeState.isSurprised) return
        eyeState.isSurprised = true
        eyeState.surpriseProgress = 0f
        animate(
            EyeConstants.SURPRISE_DURATION_MS,
            onUpdate = { t ->
                eyeState.surpriseProgress = if (t < 0.5f) t * 2f else (1f - (t - 0.5f) * 2f)
            },
            onEnd = {
                eyeState.isSurprised = false
                eyeState.surpriseProgress = 0f
            }
        )
    }
    
    /**
     * 眠そうな表情をトリガー
     */
    fun triggerSleepy() {
        if (eyeState.isSleepy) return
        eyeState.isSleepy = true
        eyeState.sleepProgress = 0f
        animate(
            EyeConstants.SLEEPY_DURATION_MS,
            onUpdate = { t ->
                eyeState.sleepProgress = if (t < 0.3f) t / 0.3f else 1f
            },
            onEnd = {
                eyeState.isSleepy = false
                eyeState.sleepProgress = 0f
            }
        )
    }
    
    /**
     * 怒りの表情をトリガー
     */
    fun triggerAngry() {
        if (eyeState.isAngry) return
        eyeState.isAngry = true
        eyeState.angryProgress = 0f
        animate(
            EyeConstants.ANGRY_DURATION_MS,
            onUpdate = { t ->
                eyeState.angryProgress = if (t < 0.5f) t * 2f else (1f - (t - 0.5f) * 2f)
            },
            onEnd = {
                eyeState.isAngry = false
                eyeState.angryProgress = 0f
            }
        )
    }
    
    /**
     * 目を細める（笑顔）をトリガー
     */
    fun triggerSquint() {
        if (eyeState.isSquinting) return
        eyeState.isSquinting = true
        eyeState.squintProgress = 0f
        animate(
            EyeConstants.SQUINT_DURATION_MS,
            onUpdate = { t ->
                eyeState.squintProgress = if (t < 0.5f) t * 2f else (1f - (t - 0.5f) * 2f)
            },
            onEnd = {
                eyeState.isSquinting = false
                eyeState.squintProgress = 0f
            }
        )
    }
    
    /**
     * 2回まばたきしてからモードを切り替え
     */
    fun triggerDoubleBlinkThenSwitch(targetMode: EyeState.EyeMode, onModeChanged: () -> Unit) {
        // 一回目
        triggerBlink()
        // 少し待って二回目
        mainHandler.postDelayed({ triggerBlink() }, EyeConstants.DOUBLE_BLINK_INTERVAL_MS)
        // さらに待ってモード切替
        mainHandler.postDelayed({
            eyeState.mode = targetMode
            onModeChanged()
        }, EyeConstants.MODE_SWITCH_DELAY_MS)
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
