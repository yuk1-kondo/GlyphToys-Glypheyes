package com.example.glypheyes

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import kotlin.math.*

// Import from SDK (provided via AAR)
import com.nothing.glyph.sdk.GlyphMatrixManager
import com.nothing.glyph.sdk.GlyphMatrixFrame
import com.nothing.glyph.sdk.GlyphMatrixObject
import com.nothing.glyph.sdk.GlyphToy
import com.nothing.glyph.sdk.Glyph

class GlyphEyesService : Service(), SensorEventListener {

    private var gm: GlyphMatrixManager? = null
    private var frameBuilder: GlyphMatrixFrame.Builder? = null

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    private var filteredX: Float = 0f
    private var filteredY: Float = 0f

    private val alpha: Float = 0.15f // Low-pass filter coefficient

    private val eyeCenterLeft = Pair(7, 12)
    private val eyeCenterRight = Pair(18, 12)
    private val eyeRadius = 4 // white eye approx radius (9 diameter)
    private val pupilRadius = 3
    private val pupilRange = 3 // max offset from center

    private var blinkProgress: Float = 0f // 0 open, 1 closed
    private var isBlinking: Boolean = false

    private var surpriseProgress: Float = 0f // 0 normal, 1 max
    private var isSurprised: Boolean = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private val serviceHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                GlyphToy.MSG_GLYPH_TOY -> {
                    val bundle = msg.data
                    val event = bundle.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
                    when (event) {
                        GlyphToy.EVENT_CHANGE -> triggerSurprise()
                        GlyphToy.EVENT_ACTION_DOWN -> triggerBlink()
                        GlyphToy.EVENT_AOD -> handleAodTick()
                    }
                }
                else -> super.handleMessage(msg)
            }
        }
    }
    private val messenger = Messenger(serviceHandler)

    override fun onBind(intent: Intent?): IBinder? {
        init()
        return messenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        teardown()
        return super.onUnbind(intent)
    }

    private fun init() {
        gm = GlyphMatrixManager()
        gm?.init(object : GlyphMatrixManager.Callback {})
        gm?.register(Glyph.DEVICE_23112)

        frameBuilder = GlyphMatrixFrame.Builder()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }

        startFrameLoop()
    }

    private fun teardown() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        accelerometer = null
        mainHandler.removeCallbacksAndMessages(null)
        gm?.unInit()
        gm = null
        frameBuilder = null
    }

    private fun startFrameLoop() {
        val frameIntervalMs = 33L // ~30 FPS
        mainHandler.post(object : Runnable {
            override fun run() {
                renderFrame()
                mainHandler.postDelayed(this, frameIntervalMs)
            }
        })
    }

    private fun triggerBlink() {
        if (isBlinking) return
        isBlinking = true
        blinkProgress = 0f
        animate(150L) { t ->
            blinkProgress = if (t < 0.5f) t * 2f else (1f - (t - 0.5f) * 2f)
        } onEnd@{
            isBlinking = false
            blinkProgress = 0f
        }
    }

    private fun triggerSurprise() {
        if (isSurprised) return
        isSurprised = true
        surpriseProgress = 0f
        animate(300L) { t ->
            surpriseProgress = if (t < 0.5f) t * 2f else (1f - (t - 0.5f) * 2f)
        } onEnd@{
            isSurprised = false
            surpriseProgress = 0f
        }
    }

    private fun handleAodTick() {
        // gentle sway
        val time = SystemClock.uptimeMillis() / 1000f
        filteredX = sin(time * 0.5f) * 0.3f
        filteredY = cos(time * 0.5f) * 0.3f
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val ax = event.values[0]
        val ay = event.values[1]
        // Normalize and invert as needed so that tilting right moves pupils right
        val nx = (-ax / 5f).coerceIn(-1f, 1f)
        val ny = (ay / 5f).coerceIn(-1f, 1f)
        filteredX += alpha * (nx - filteredX)
        filteredY += alpha * (ny - filteredY)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun renderFrame() {
        val builder = GlyphMatrixFrame.Builder()

        val scaleBoost = 1f + 0.3f * surpriseProgress
        val eyeR = (eyeRadius * scaleBoost).roundToInt().coerceAtLeast(3)

        // Draw white eyes (low layer)
        builder.addLow(drawCircleObject(eyeCenterLeft.first - eyeR, eyeCenterLeft.second - eyeR, eyeR * 2, 255))
        builder.addLow(drawCircleObject(eyeCenterRight.first - eyeR, eyeCenterRight.second - eyeR, eyeR * 2, 255))

        // Pupils position
        val offsetX = (filteredX * pupilRange).roundToInt()
        val offsetY = (filteredY * pupilRange).roundToInt()
        val leftPupilCenter = Pair(eyeCenterLeft.first + offsetX, eyeCenterLeft.second + offsetY)
        val rightPupilCenter = Pair(eyeCenterRight.first + offsetX, eyeCenterRight.second + offsetY)

        // Clamp pupils within eye radius
        fun clampToEye(center: Pair<Int, Int>, target: Pair<Int, Int>): Pair<Int, Int> {
            val dx = (target.first - center.first).toFloat()
            val dy = (target.second - center.second).toFloat()
            val dist = sqrt(dx * dx + dy * dy)
            val maxD = (eyeR - pupilRadius).toFloat()
            if (dist <= maxD || dist == 0f) return target
            val scale = maxD / dist
            return Pair(center.first + (dx * scale).roundToInt(), center.second + (dy * scale).roundToInt())
        }
        val clampedLeft = clampToEye(eyeCenterLeft, leftPupilCenter)
        val clampedRight = clampToEye(eyeCenterRight, rightPupilCenter)

        // Pupils (mid layer)
        builder.addMid(drawFilledCircleObject(clampedLeft.first - pupilRadius, clampedLeft.second - pupilRadius, pupilRadius * 2, 0))
        builder.addMid(drawFilledCircleObject(clampedRight.first - pupilRadius, clampedRight.second - pupilRadius, pupilRadius * 2, 0))

        // Eyelid blink (top layer) as a horizontal black bar descending
        if (isBlinking || blinkProgress > 0f) {
            val cover = (eyeR * blinkProgress).roundToInt()
            builder.addTop(drawRectObject(0, eyeCenterLeft.second - eyeR, 25, cover, 0))
            builder.addTop(drawRectObject(0, eyeCenterLeft.second + eyeR - cover, 25, cover, 0))
        }

        val frame = builder.build(this)
        gm?.setMatrixFrame(frame.render())
    }

    // Helpers to create simple shapes as GlyphMatrixObject via text bitmaps
    private fun drawCircleObject(x: Int, y: Int, size: Int, brightness: Int): GlyphMatrixObject {
        val objBuilder = GlyphMatrixObject.Builder()
        // Using text as a placeholder; the SDK example suggests setText works to render
        objBuilder.setText("O")
        objBuilder.setPosition(x, y)
        objBuilder.setScale(size * 10) // heuristic; depends on SDK renderer
        objBuilder.setBrightness(brightness)
        return objBuilder.build()
    }

    private fun drawFilledCircleObject(x: Int, y: Int, size: Int, brightness: Int): GlyphMatrixObject {
        val objBuilder = GlyphMatrixObject.Builder()
        objBuilder.setText("@")
        objBuilder.setPosition(x, y)
        objBuilder.setScale(size * 10)
        objBuilder.setBrightness(255 - brightness)
        return objBuilder.build()
    }

    private fun drawRectObject(x: Int, y: Int, width: Int, height: Int, brightness: Int): GlyphMatrixObject {
        val objBuilder = GlyphMatrixObject.Builder()
        objBuilder.setText("#")
        objBuilder.setPosition(x, y)
        objBuilder.setScale(max(width, height) * 10)
        objBuilder.setBrightness(brightness)
        return objBuilder.build()
    }

    private fun animate(durationMs: Long, onUpdate: (Float) -> Unit, onEnd: () -> Unit) {
        val start = SystemClock.uptimeMillis()
        fun step() {
            val t = ((SystemClock.uptimeMillis() - start).toFloat() / durationMs).coerceIn(0f, 1f)
            onUpdate(t)
            if (t < 1f) {
                mainHandler.postDelayed({ step() }, 16L)
            } else {
                onEnd()
            }
        }
        step()
    }
}


