package com.example.glypheyes

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import kotlin.math.roundToInt

// Import from SDK (provided via AAR)
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixObject
import com.nothing.ketchum.GlyphToy
import com.nothing.ketchum.Glyph

/**
 * Glyph Eyes サービス（リファクタ版）
 * 各コンポーネントを組み合わせて、目の表情を制御するコーディネーター
 */
class GlyphEyesService : Service() {

    companion object {
        private const val TAG = "GlyphEyesService"
    }

    // コンポーネント
    private lateinit var eyeState: EyeState
    private lateinit var animationController: AnimationController
    private lateinit var tiltSensorManager: TiltSensorManager
    private lateinit var batteryMonitor: BatteryMonitor
    private lateinit var demoManager: DemoManager
    private lateinit var eyeRenderer: EyeRenderer

    // Glyph SDK
    private var gm: GlyphMatrixManager? = null
    private var frameBuilder: GlyphMatrixFrame.Builder? = null

    // Handler for animations and frame updates
    private val mainHandler = Handler(Looper.getMainLooper())

    // 目の形状定数
    private val eyeCenterLeft = Pair(EyeConstants.EYE_CENTER_LEFT_X, EyeConstants.EYE_CENTER_LEFT_Y)
    private val eyeCenterRight = Pair(EyeConstants.EYE_CENTER_RIGHT_X, EyeConstants.EYE_CENTER_RIGHT_Y)
    private val eyeRadiusX = EyeConstants.EYE_RADIUS_X
    private val eyeRadiusY = EyeConstants.EYE_RADIUS_Y
    private val pupilRadius = EyeConstants.PUPIL_RADIUS
    private val pupilRange = EyeConstants.PUPIL_RANGE

    // DEBUG: force sleepy demo regardless of battery
    private val demoForceSleepy: Boolean = false

    private val serviceHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                GlyphToy.MSG_GLYPH_TOY -> {
                    val bundle = msg.data
                    val event = bundle.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
                    when (event) {
                        GlyphToy.EVENT_CHANGE -> animationController.triggerSurprise()
                        GlyphToy.EVENT_ACTION_DOWN -> handleButtonDown()
                        GlyphToy.EVENT_ACTION_UP -> handleButtonUp()
                        GlyphToy.EVENT_AOD -> {
                            demoManager.handleAodTick()
                            // Randomly trigger different emotions during AOD
                            val random = (0..100).random()
                            when {
                                random < 5 -> animationController.triggerSleepy()
                                random < 10 -> animationController.triggerAngry()
                            }
                        }
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
        Log.d(TAG, "Initializing service...")
        
        // エラーハンドリング用のコールバック
        val errorCallback: ErrorCallback = { component, error, exception ->
            Log.e(TAG, "[$component] $error", exception)
        }
        
        // コンポーネントを初期化
        eyeState = EyeState()
        animationController = AnimationController(eyeState)
        tiltSensorManager = TiltSensorManager(this, eyeState, errorCallback)
        batteryMonitor = BatteryMonitor(this, eyeState, animationController, errorCallback)
        demoManager = DemoManager(eyeState, errorCallback)
        eyeRenderer = EyeRenderer()

        // Glyph SDK 初期化
        gm = GlyphMatrixManager.getInstance(this)
        gm?.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(name: android.content.ComponentName?) {
                Log.d(TAG, "Glyph service connected")
            }
            override fun onServiceDisconnected(name: android.content.ComponentName?) {
                Log.w(TAG, "Glyph service disconnected")
            }
        })
        gm?.register(Glyph.DEVICE_23112)

        frameBuilder = GlyphMatrixFrame.Builder()

        // コンポーネントを開始
        val sensorResult = tiltSensorManager.start()
        if (sensorResult is InitResult.Failure) {
            Log.w(TAG, "Sensor initialization failed: ${sensorResult.reason}")
        }

        val batteryResult = batteryMonitor.start()
        if (batteryResult is InitResult.Failure) {
            Log.w(TAG, "Battery monitor initialization failed: ${batteryResult.reason}")
        }

        // デモ用の強制眠気モード（デバッグ用）
        if (demoForceSleepy) {
            eyeState.batteryMoodTier = 2
            eyeState.batterySleepBias = 0.6f
            animationController.triggerSleepy()
        }

        // デモモーション開始
        val demoResult = demoManager.start()
        if (demoResult is InitResult.Failure) {
            Log.w(TAG, "Demo manager initialization failed: ${demoResult.reason}")
        }

        // フレーム描画開始
        startFrameLoop()
        
        Log.i(TAG, "Service initialized successfully")
    }

    private fun teardown() {
        Log.d(TAG, "Tearing down service...")
        tiltSensorManager.stop()
        batteryMonitor.stop()
        demoManager.stop()
        mainHandler.removeCallbacksAndMessages(null)
        gm?.unInit()
        gm = null
        frameBuilder = null
        Log.i(TAG, "Service teardown complete")
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

    private fun renderFrame() {
        val builder = GlyphMatrixFrame.Builder()

        // Calculate eye size based on emotions
        val scaleBoost = 1f + 0.3f * eyeState.surpriseProgress
        val effectiveSleep = eyeState.getEffectiveSleep()
        val angryScale = 1f + 0.2f * eyeState.angryProgress
        val squintScale = 1f - 0.6f * eyeState.squintProgress
        
        val finalScale = scaleBoost * angryScale * squintScale
        val eyeRX = (eyeRadiusX * finalScale).roundToInt().coerceAtLeast(1)
        val eyeRY = (eyeRadiusY * finalScale).roundToInt().coerceAtLeast(1)

        // 瞳オフセット（モード別）
        val (lx, ly, rx, ry) = when (eyeState.mode) {
            EyeState.EyeMode.SENSOR -> {
                val ox = (eyeState.filteredX * pupilRange).roundToInt()
                val oy = (eyeState.filteredY * pupilRange).roundToInt()
                DemoManager.Quad(ox, oy, ox, oy)
            }
            EyeState.EyeMode.DEMO -> {
                val now = SystemClock.uptimeMillis()
                if (now - eyeState.demoStartAt >= eyeState.demoDurationMs) {
                    demoManager.scheduleNextDemoMotion()
                }
                demoManager.calculateDemoPosition()
            }
        }

        val pupilLeftCenter = Pair(eyeCenterLeft.first + lx, eyeCenterLeft.second + ly)
        val pupilRightCenter = Pair(eyeCenterRight.first + rx, eyeCenterRight.second + ry)

        // Apply ellipse clamping to prevent overflow
        val clL = eyeRenderer.clamp(eyeCenterLeft, pupilLeftCenter, eyeRX, pupilRadius)
        val clR = eyeRenderer.clamp(eyeCenterRight, pupilRightCenter, eyeRX, pupilRadius)

        // 目のリングと瞳を1枚のビットマップに描画
        val bm = eyeRenderer.drawEyes(
            leftEyeCenter = eyeCenterLeft,
            rightEyeCenter = eyeCenterRight,
            eyeRadiusX = eyeRX,
            eyeRadiusY = eyeRY,
            pupilLeftCenter = clL,
            pupilRightCenter = clR,
            pupilRadius = pupilRadius,
            blink = eyeState.blinkProgress,
            sleep = effectiveSleep,
            angry = eyeState.angryProgress,
            squint = eyeState.squintProgress
        )

        // ビットマップから Glyph オブジェクトを作成
        val eyesObj = GlyphMatrixObject.Builder()
            .setImageSource(bm)
            .setPosition(0, 0)
            .setScale(100)
            .setBrightness(255)
            .build()

        builder.addMid(eyesObj)

        val frame = builder.build(this)
        gm?.setMatrixFrame(frame.render())
    }

    private fun handleButtonDown() {
        eyeState.lastActionDownAt = SystemClock.uptimeMillis()
    }

    private fun handleButtonUp() {
        val now = SystemClock.uptimeMillis()
        val dur = now - eyeState.lastActionDownAt
        if (dur >= EyeConstants.LONG_PRESS_THRESHOLD_MS) {
            // 長押し: 2回まばたき → モードをトグル
            val target = if (eyeState.mode == EyeState.EyeMode.DEMO) {
                EyeState.EyeMode.SENSOR
            } else {
                EyeState.EyeMode.DEMO
            }
            animationController.triggerDoubleBlinkThenSwitch(target) {
                if (target == EyeState.EyeMode.DEMO) {
                    // デモに戻るときはパターンをリセット
                    demoManager.scheduleNextDemoMotion()
                }
            }
        } else {
            // 短押し: 軽い表情
            animationController.triggerSquint()
        }
    }
}
