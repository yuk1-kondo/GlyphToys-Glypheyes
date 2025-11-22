package com.example.glypheyes

import android.util.Pair

/**
 * 目の現在の状態を保持するデータクラス
 * 瞳の位置、表情の進行度、動作モードなどを管理
 */
data class EyeState(
    // 動作モード
    var mode: EyeMode = EyeMode.DEMO,
    
    // 瞳の位置（センサー入力のフィルタリング済み値）
    var filteredX: Float = 0f,
    var filteredY: Float = 0f,
    
    // 表情アニメーションの進行度（0.0〜1.0）
    var blinkProgress: Float = 0f,
    var surpriseProgress: Float = 0f,
    var sleepProgress: Float = 0f,
    var angryProgress: Float = 0f,
    var squintProgress: Float = 0f,
    
    // アニメーション実行中フラグ
    var isBlinking: Boolean = false,
    var isSurprised: Boolean = false,
    var isSleepy: Boolean = false,
    var isAngry: Boolean = false,
    var isSquinting: Boolean = false,
    
    // バッテリー由来の眠気バイアス（持続的）
    var batterySleepBias: Float = 0f,
    var batteryMoodTier: Int = 0, // 0=通常, 1=<30%, 2=<15%, 3=充電中
    
    // デモモード用の状態
    var demoType: DemoType = DemoType.LR,
    var demoStartAt: Long = 0L,
    var demoDurationMs: Long = 3000L,
    var demoSeed: Float = 0f,
    
    // AOD用のランダム位置
    var aodLastChangeAt: Long = 0L,
    var aodOffsetX: Float = 0f,
    var aodOffsetY: Float = 0f,
    
    // ボタン操作
    var lastActionDownAt: Long = 0L
) {
    /**
     * 動作モード
     */
    enum class EyeMode {
        DEMO,   // デモパターンで自動的に動く
        SENSOR  // センサー（加速度計）に従って動く
    }
    
    /**
     * デモパターンの種類
     */
    enum class DemoType {
        LR,        // 左右同期
        UD,        // 上下同期
        CROSSEYE,  // 寄り目
        APART,     // 離れ目
        DRIFT,     // ランダムドリフト
        STOP       // 停止（中央）
    }
    
    /**
     * 有効な眠気の進行度を取得（一時的な眠気 + バッテリー由来）
     */
    fun getEffectiveSleep(): Float {
        return (sleepProgress + batterySleepBias).coerceIn(0f, 1f)
    }
    
    /**
     * 瞳の位置を更新（センサー値のフィルタリング）
     */
    fun updateSensorInput(nx: Float, ny: Float) {
        filteredX += EyeConstants.LOWPASS_ALPHA * (nx - filteredX)
        filteredY += EyeConstants.LOWPASS_ALPHA * (ny - filteredY)
    }
    
    /**
     * AOD用の固定位置を適用
     */
    fun applyAodPosition() {
        filteredX = aodOffsetX
        filteredY = aodOffsetY
    }
}
