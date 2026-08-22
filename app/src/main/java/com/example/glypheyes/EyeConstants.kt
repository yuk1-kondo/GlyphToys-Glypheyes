package com.example.glypheyes

/**
 * GlyphEyes アプリケーション全体で使用する定数定義
 * マジックナンバーを排除し、可読性と保守性を向上させる
 */
object EyeConstants {
    
    // ===== アニメーション時間 =====
    /** まばたきアニメーションの時間（ミリ秒） */
    const val BLINK_DURATION_MS = 150L
    
    /** 眠気アニメーションの時間（ミリ秒） */
    const val SLEEPY_DURATION_MS = 2000L
    
    /** 驚きアニメーションの時間（ミリ秒） */
    const val SURPRISE_DURATION_MS = 300L
    
    /** 怒りアニメーションの時間（ミリ秒） */
    const val ANGRY_DURATION_MS = 800L
    
    /** 目を細める（笑顔）アニメーションの時間（ミリ秒） */
    const val SQUINT_DURATION_MS = 800L

    /** ウインク（左目だけ閉じる）アニメーションの時間（ミリ秒） */
    const val WINK_DURATION_MS = 900L

    /** 充電開始時の「驚き」から「笑顔」への遷移待ち時間（ミリ秒） */
    const val WAKEUP_SMILE_DELAY_MS = 400L
    
    /** 2回まばたきの間隔（ミリ秒） */
    const val DOUBLE_BLINK_INTERVAL_MS = 240L
    
    /** 2回まばたき後のモード切替までの待ち時間（ミリ秒） */
    const val MODE_SWITCH_DELAY_MS = 520L
    
    // ===== センサー処理 =====
    /** ローパスフィルタの係数（0.0〜1.0、小さいほどスムーズ） */
    const val LOWPASS_ALPHA = 0.15f
    
    // ===== デモモード =====
    /** デモ動作の最小時間（ミリ秒） */
    const val DEMO_MIN_DURATION_MS = 1500L
    
    /** デモ動作の最大時間（ミリ秒） */
    const val DEMO_MAX_DURATION_MS = 4000L
    
    /** AOD（Always On Display）での目の位置変更間隔（ミリ秒） */
    const val AOD_INTERVAL_MS = 60_000L // 1分

    // ※ 目の形状・瞳の移動制限はデバイスのマトリクス長に依存するため
    //    EyeLayout（EyeLayout.forMatrixLength）で管理する

    // ===== ボタン操作 =====
    /** 長押しと判定する閾値（ミリ秒） */
    const val LONG_PRESS_THRESHOLD_MS = 700L

    /** ダブルプレス（2連続短押し）と判定する間隔（ミリ秒） */
    const val DOUBLE_PRESS_WINDOW_MS = 300L
    
    // ===== バッテリー管理 =====
    /** バッテリー残量が少ないと判定する閾値（%） */
    const val BATTERY_LOW_THRESHOLD = 30
    
    /** バッテリー残量が危機的に少ないと判定する閾値（%） */
    const val BATTERY_CRITICAL_THRESHOLD = 15
    
    // ===== フレームレート =====
    /** アニメーションフレーム間隔（ミリ秒、約60fps） */
    const val ANIMATION_FRAME_INTERVAL_MS = 16L

    // ===== デモパターンの確率 =====
    /** デモで停止する確率（%） */
    const val DEMO_STOP_PROBABILITY = 10
    
    /** デモで同方向に動く確率（%） */
    const val DEMO_SYNCHRONIZED_PROBABILITY = 67 // 残り90%のうち約3/4
}
