package com.example.glypheyes

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

/**
 * 加速度センサーを管理し、端末の傾きを検出するクラス
 */
class TiltSensorManager(
    context: Context,
    private val eyeState: EyeState,
    private val onError: ErrorCallback? = null
) : SensorEventListener, ManagedComponent {
    
    private val sensorManager: SensorManager? = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var isActive = false
    
    companion object {
        private const val TAG = "TiltSensorManager"
    }
    
    /**
     * センサーのリスニングを開始
     */
    override fun start(): InitResult {
        return try {
            if (sensorManager == null) {
                val msg = "SensorManager is not available"
                Log.e(TAG, msg)
                onError?.invoke(TAG, msg, null)
                return InitResult.Failure(msg)
            }
            
            if (accelerometer == null) {
                val msg = "Accelerometer sensor is not available"
                Log.w(TAG, msg)
                onError?.invoke(TAG, msg, null)
                return InitResult.Failure(msg)
            }
            
            val registered = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            if (registered) {
                isActive = true
                Log.d(TAG, "Sensor started successfully")
                InitResult.Success
            } else {
                val msg = "Failed to register sensor listener"
                Log.e(TAG, msg)
                onError?.invoke(TAG, msg, null)
                InitResult.Failure(msg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting sensor", e)
            onError?.invoke(TAG, "Exception during start", e)
            InitResult.Failure("Exception: ${e.message}", e)
        }
    }
    
    /**
     * センサーのリスニングを停止
     */
    override fun stop() {
        try {
            sensorManager?.unregisterListener(this)
            isActive = false
            Log.d(TAG, "Sensor stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping sensor", e)
            onError?.invoke(TAG, "Exception during stop", e)
        }
    }
    
    override fun isActive(): Boolean = isActive
    
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        
        val ax = event.values[0]
        val ay = event.values[1]
        
        // X軸の修正: 端末を右に傾けたら（ax正）、瞳も右へ（nx正）
        val nx = (ax / 3f).coerceIn(-1f, 1f) // 感度調整済み
        
        // Y軸の修正: 端末を上に傾けたら（ay負）、瞳も上へ（ny負）
        val ny = (ay / 3f).coerceIn(-1f, 1f) // 感度調整済み
        
        // EyeStateに反映（ローパスフィルタ適用）
        eyeState.updateSensorInput(nx, ny)
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 精度変更時の処理（特に何もしない）
    }
}
