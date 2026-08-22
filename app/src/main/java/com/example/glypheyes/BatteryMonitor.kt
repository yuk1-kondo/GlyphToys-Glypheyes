package com.example.glypheyes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log

/**
 * バッテリー状態を監視し、眠気バイアスを管理するクラス
 */
class BatteryMonitor(
    private val context: Context,
    private val eyeState: EyeState,
    private val animationController: AnimationController,
    private val onError: ErrorCallback? = null
) : ManagedComponent {
    
    private var batteryReceiver: BroadcastReceiver? = null
    private var wasBatteryLow: Boolean = false
    private var isActive = false
    
    companion object {
        private const val TAG = "BatteryMonitor"
    }
    
    /**
     * バッテリー監視を開始
     */
    override fun start(): InitResult {
        return try {
            registerBatteryReceiver()
            
            // 初回のバッテリー状態を取得
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val sticky = context.registerReceiver(null, filter)
            sticky?.let { batteryReceiver?.onReceive(context, it) }
            
            isActive = true
            Log.d(TAG, "Battery monitor started successfully")
            InitResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error starting battery monitor", e)
            onError?.invoke(TAG, "Exception during start", e)
            InitResult.Failure("Exception: ${e.message}", e)
        }
    }
    
    /**
     * バッテリー監視を停止
     */
    override fun stop() {
        batteryReceiver?.let {
            try {
                context.unregisterReceiver(it)
                Log.d(TAG, "Battery monitor stopped successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Receiver already unregistered", e)
            }
        }
        batteryReceiver = null
        isActive = false
    }
    
    override fun isActive(): Boolean = isActive
    
    /**
     * バッテリー状態の変化を監視するレシーバーを登録
     */
    private fun registerBatteryReceiver() {
        if (batteryReceiver != null) return
        
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level < 0 || scale <= 0) return
                
                val percent = (level * 100f / scale.toFloat()).toInt()
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                                status == BatteryManager.BATTERY_STATUS_FULL || 
                                plugged != 0
                
                // バッテリー状態に応じてティアを決定
                val newTier = when {
                    isCharging -> 3
                    percent < EyeConstants.BATTERY_CRITICAL_THRESHOLD -> 2
                    percent < EyeConstants.BATTERY_LOW_THRESHOLD -> 1
                    else -> 0
                }
                
                if (newTier != eyeState.batteryMoodTier) {
                    eyeState.batteryMoodTier = newTier
                    when (eyeState.batteryMoodTier) {
                        2 -> { // 強い眠気
                            eyeState.batterySleepBias = 0.6f
                            animationController.triggerSleepy()
                            wasBatteryLow = true
                        }
                        1 -> { // 軽い眠気
                            eyeState.batterySleepBias = 0.3f
                            animationController.triggerSleepy()
                            wasBatteryLow = true
                        }
                        3 -> { // 充電中 → 眠気解除して笑顔
                            if (wasBatteryLow) {
                                eyeState.batterySleepBias = 0f
                                animationController.triggerWakeupSmile() // 驚き→笑顔で目覚める
                                wasBatteryLow = false
                            }
                        }
                        else -> { // 通常
                            eyeState.batterySleepBias = 0f
                            wasBatteryLow = false
                        }
                    }
                }
            }
        }
        
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
    }
}
