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
    
    // ===== 目の形状 =====
    /** 左目の中心座標 X */
    const val EYE_CENTER_LEFT_X = 8
    
    /** 左目の中心座標 Y */
    const val EYE_CENTER_LEFT_Y = 12
    
    /** 右目の中心座標 X */
    const val EYE_CENTER_RIGHT_X = 17
    
    /** 右目の中心座標 Y */
    const val EYE_CENTER_RIGHT_Y = 12
    
    /** 目の横方向の半径（ピクセル） */
    const val EYE_RADIUS_X = 4
    
    /** 目の縦方向の半径（ピクセル、縦長の楕円） */
    const val EYE_RADIUS_Y = 6
    
    /** 瞳の半径（ピクセル） */
    const val PUPIL_RADIUS = 2
    
    /** 瞳が中心から移動できる最大距離（ピクセル） */
    const val PUPIL_RANGE = 4
    
    /** 目の輪郭線の太さ（ピクセル） */
    const val EYE_OUTLINE_WIDTH = 1
    
    // ===== 瞳の移動制限 =====
    /** 瞳の右方向への最大移動量 */
    const val PUPIL_LIMIT_RIGHT = 4
    
    /** 瞳の左方向への最大移動量（負の値） */
    const val PUPIL_LIMIT_LEFT = -2
    
    /** 瞳の下方向への最大移動量 */
    const val PUPIL_LIMIT_DOWN = 2
    
    /** 瞳の上方向への最大移動量（負の値） */
    const val PUPIL_LIMIT_UP = -5
    
    // ===== ボタン操作 =====
    /** 長押しと判定する閾値（ミリ秒） */
    const val LONG_PRESS_THRESHOLD_MS = 700L
    
    // ===== バッテリー管理 =====
    /** バッテリー残量が少ないと判定する閾値（%） */
    const val BATTERY_LOW_THRESHOLD = 30
    
    /** バッテリー残量が危機的に少ないと判定する閾値（%） */
    const val BATTERY_CRITICAL_THRESHOLD = 15
    
    // ===== フレームレート =====
    /** アニメーションフレーム間隔（ミリ秒、約60fps） */
    const val ANIMATION_FRAME_INTERVAL_MS = 16L
    
    // ===== ビットマップサイズ =====
    /** 目を描画するビットマップの幅（ピクセル） */
    const val BITMAP_WIDTH = 25
    
    /** 目を描画するビットマップの高さ（ピクセル） */
    const val BITMAP_HEIGHT = 25
    
    // ===== デモパターンの確率 =====
    /** デモで停止する確率（%） */
    const val DEMO_STOP_PROBABILITY = 10
    
    /** デモで同方向に動く確率（%） */
    const val DEMO_SYNCHRONIZED_PROBABILITY = 67 // 残り90%のうち約3/4
}
