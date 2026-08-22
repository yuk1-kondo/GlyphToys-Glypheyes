package com.example.glypheyes

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import kotlin.math.*

/**
 * デモモードのパターン生成とAOD(Always On Display)処理を管理するクラス
 * @param layout デバイスのマトリクス長に応じた目のジオメトリ
 */
class DemoManager(
    private val eyeState: EyeState,
    private val layout: EyeLayout,
    private val onError: ErrorCallback? = null
) : ManagedComponent {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isActive = false

    /** デモ振幅のスケール（25px基準のチューニング値をデバイス解像度へ合わせる） */
    private val demoScale = layout.size / 25f
    
    companion object {
        private const val TAG = "DemoManager"
    }
    
    override fun start(): InitResult {
        return try {
            scheduleNextDemoMotion()
            isActive = true
            Log.d(TAG, "Demo manager started successfully")
            InitResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error starting demo manager", e)
            onError?.invoke(TAG, "Exception during start", e)
            InitResult.Failure("Exception: ${e.message}", e)
        }
    }
    
    override fun stop() {
        try {
            mainHandler.removeCallbacksAndMessages(null)
            isActive = false
            Log.d(TAG, "Demo manager stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping demo manager", e)
            onError?.invoke(TAG, "Exception during stop", e)
        }
    }
    
    override fun isActive(): Boolean = isActive
    
    /**
     * 次のデモ動作をスケジュール
     */
    fun scheduleNextDemoMotion() {
        val now = SystemClock.uptimeMillis()
        eyeState.demoStartAt = now
        
        // 1.5〜4.0秒のゆっくり区間
        eyeState.demoDurationMs = (EyeConstants.DEMO_MIN_DURATION_MS..EyeConstants.DEMO_MAX_DURATION_MS).random()
        
        // 重み付け:
        //  - STOP: 約10%
        //  - 残りは「同方向(Left-Right/Up-Down) : 左右バラバラ(Cross/Apart/Drift) ≈ 3 : 1」
        val r = (0..99).random()
        eyeState.demoType = if (r < EyeConstants.DEMO_STOP_PROBABILITY) {
            EyeState.DemoType.STOP
        } else {
            // 残り90の中で同方向を約67%(つまり全体の約60%)
            if (r < EyeConstants.DEMO_SYNCHRONIZED_PROBABILITY) {
                // 同方向: LR or UD
                if ((0..1).random() == 0) EyeState.DemoType.LR else EyeState.DemoType.UD
            } else {
                // バラバラ: CROSSEYE, APART, DRIFT
                listOf(EyeState.DemoType.CROSSEYE, EyeState.DemoType.APART, EyeState.DemoType.DRIFT).random()
            }
        }
        
        eyeState.demoSeed = (0..1000).random().toFloat()
    }
    
    /**
     * デモパターンに基づいて瞳の位置を計算
     * @return Quad(leftX, leftY, rightX, rightY)
     */
    fun calculateDemoPosition(): Quad {
        val now = SystemClock.uptimeMillis()
        val elapsed = now - eyeState.demoStartAt
        val t = (elapsed.toFloat() / eyeState.demoDurationMs).coerceIn(0f, 1f)
        val twoPi = (2.0 * PI).toFloat()
        
        return when (eyeState.demoType) {
            EyeState.DemoType.LR -> {
                // 左右同期で動く
                val phase = twoPi * t
                val x = (sin(phase * 0.5f + eyeState.demoSeed) * 3f * demoScale).roundToInt()
                val (lx, ly) = applyDirectionLimits(x, 0)
                Quad(lx, ly, lx, ly)
            }
            EyeState.DemoType.UD -> {
                // 上下同期で動く
                val phase = twoPi * t
                val y = (cos(phase * 0.5f + eyeState.demoSeed) * 2f * demoScale).roundToInt()
                val (lx, ly) = applyDirectionLimits(0, y)
                Quad(lx, ly, lx, ly)
            }
            EyeState.DemoType.CROSSEYE -> {
                // 寄り目（左右で逆方向）
                val phase = twoPi * t
                val baseX = (sin(phase * 0.6f + eyeState.demoSeed) * 3f * demoScale).roundToInt()
                val (lx, ly) = applyDirectionLimits(baseX, 0)
                val (rx, ry) = applyDirectionLimits(-baseX, 0)
                Quad(lx, ly, rx, ry)
            }
            EyeState.DemoType.APART -> {
                // 離れ目（左右で同方向だが大きめ）
                val phase = twoPi * t
                val baseX = (sin(phase * 0.4f + eyeState.demoSeed) * 4f * demoScale).roundToInt()
                val (lx, ly) = applyDirectionLimits(baseX, 0)
                Quad(lx, ly, lx, ly)
            }
            EyeState.DemoType.DRIFT -> {
                // 低速・低振幅の左右独立ドリフト
                val phase = twoPi * t
                val xL = (sin(phase * 0.6f + eyeState.demoSeed) * 2f * demoScale).roundToInt()
                val yL = (cos(phase * 0.4f + eyeState.demoSeed) * 1f * demoScale).roundToInt()
                val xR = (sin(phase * 0.6f + eyeState.demoSeed + PI.toFloat()) * 2f * demoScale).roundToInt()
                val yR = (cos(phase * 0.4f + eyeState.demoSeed + PI.toFloat()) * 1f * demoScale).roundToInt()
                val (lx, ly) = applyDirectionLimits(xL, yL)
                val (rx, ry) = applyDirectionLimits(xR, yR)
                Quad(lx, ly, rx, ry)
            }
            EyeState.DemoType.STOP -> {
                Quad(0, 0, 0, 0)
            }
        }
    }
    
    /**
     * 方向ごとの上限を適用
     */
    private fun applyDirectionLimits(dx: Int, dy: Int): Pair<Int, Int> {
        val limitedX = when {
            dx > 0 -> minOf(dx, layout.limitRight)
            dx < 0 -> maxOf(dx, layout.limitLeft)
            else -> dx
        }
        val limitedY = when {
            dy > 0 -> minOf(dy, layout.limitDown)
            dy < 0 -> maxOf(dy, layout.limitUp)
            else -> dy
        }
        return Pair(limitedX, limitedY)
    }
    
    /**
     * AOD（Always On Display）用のランダム位置を生成
     */
    fun generateRandomAodPosition() {
        val patterns = listOf("LR", "UD", "CROSSEYE", "APART", "DRIFT", "STOP")
        val pattern = patterns.random()
        
        when (pattern) {
            "LR" -> {
                // 左右どちらかの端へ
                val direction = if ((0..1).random() == 0) -1f else 1f
                eyeState.aodOffsetX = direction * 0.8f
                eyeState.aodOffsetY = 0f
            }
            "UD" -> {
                // 上下どちらかへ
                val direction = if ((0..1).random() == 0) -1f else 1f
                eyeState.aodOffsetX = 0f
                eyeState.aodOffsetY = direction * 0.5f
            }
            "CROSSEYE" -> {
                // 寄り目（右寄り）
                eyeState.aodOffsetX = 0.6f
                eyeState.aodOffsetY = 0f
            }
            "APART" -> {
                // 離れ目（左寄り）
                eyeState.aodOffsetX = -0.4f
                eyeState.aodOffsetY = 0f
            }
            "DRIFT" -> {
                // ランダムな位置
                eyeState.aodOffsetX = ((-8..8).random() / 10f)
                eyeState.aodOffsetY = ((-4..4).random() / 10f)
            }
            "STOP" -> {
                // 中央
                eyeState.aodOffsetX = 0f
                eyeState.aodOffsetY = 0f
            }
        }
    }
    
    /**
     * AODのティックを処理（1分ごとに位置変更）
     */
    fun handleAodTick() {
        val now = SystemClock.uptimeMillis()
        
        // 1分間隔でランダムな目位置に変更
        if (now - eyeState.aodLastChangeAt >= EyeConstants.AOD_INTERVAL_MS) {
            eyeState.aodLastChangeAt = now
            generateRandomAodPosition()
        }
        
        // AOD用の固定位置を適用
        eyeState.applyAodPosition()
    }
    
    /**
     * 瞳の位置を表すデータクラス（左右の目それぞれのX, Y座標）
     */
    data class Quad(val lx: Int, val ly: Int, val rx: Int, val ry: Int)
}
