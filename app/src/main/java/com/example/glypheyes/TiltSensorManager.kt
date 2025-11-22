package com.example.glypheyes

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * 加速度センサーを管理し、端末の傾きを検出するクラス
 */
class TiltSensorManager(
    context: Context,
    private val eyeState: EyeState
) : SensorEventListener {
    
    private val sensorManager: SensorManager? = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    /**
     * センサーのリスニングを開始
     */
    fun start() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }
    
    /**
     * センサーのリスニングを停止
     */
    fun stop() {
        sensorManager?.unregisterListener(this)
    }
    
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
