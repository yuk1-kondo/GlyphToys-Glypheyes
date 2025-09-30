package com.example.glypheyes

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.os.BatteryManager
import kotlin.math.*
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

// Import from SDK (provided via AAR)
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixObject
import com.nothing.ketchum.GlyphToy
import com.nothing.ketchum.Glyph

class GlyphEyesService : Service(), SensorEventListener {

    // 動作モード（起動時はデモ → ロングプレスでセンサー追従）
    private enum class EyeMode { DEMO, SENSOR }
    private var eyeMode: EyeMode = EyeMode.DEMO

    private var gm: GlyphMatrixManager? = null
    private var frameBuilder: GlyphMatrixFrame.Builder? = null

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    private var batteryReceiver: BroadcastReceiver? = null
    private var wasBatteryLow: Boolean = false

    private var filteredX: Float = 0f
    private var filteredY: Float = 0f

    private val alpha: Float = 0.15f // Low-pass filter coefficient

    private val eyeCenterLeft = Pair(8, 12)
    private val eyeCenterRight = Pair(17, 12)
    private val eyeRadiusX = 4 // horizontal radius
    private val eyeRadiusY = 6 // vertical radius (taller)
    private val pupilRadius = 2 // smaller pupils
    private val pupilRange = 4 // max offset from center (increased for more movement)

    private var blinkProgress: Float = 0f // 0 open, 1 closed
    private var isBlinking: Boolean = false

    private var surpriseProgress: Float = 0f // 0 normal, 1 max
    private var isSurprised: Boolean = false
    
    private var sleepProgress: Float = 0f // 0 awake, 1 sleepy
    private var isSleepy: Boolean = false
    
    private var angryProgress: Float = 0f // 0 normal, 1 angry
    private var isAngry: Boolean = false
    
    private var squintProgress: Float = 0f // 0 normal, 1 squinting (laughing eyes)
    private var isSquinting: Boolean = false

    // 長押し判定
    private var lastActionDownAt: Long = 0L
    private val longPressThresholdMs: Long = 700L

    // デモ動作用
    private enum class DemoType { LR, UD, CROSSEYE, APART, DRIFT, STOP }
    private var demoType: DemoType = DemoType.LR
    private var demoStartAt: Long = 0L
    private var demoDurationMs: Long = 3000L
    private var demoSeed: Float = 0f

    // AOD用の1分間隔ランダム目位置
    private var aodLastChangeAt: Long = 0L
    private val aodIntervalMs: Long = 60000L // 1分
    private var aodOffsetX: Float = 0f
    private var aodOffsetY: Float = 0f

    // Battery-driven sleepy bias (persistent, independent of transient animations)
    private var batterySleepBias: Float = 0f // 0..1
    private var batteryMoodTier: Int = 0 // 0 normal, 1 <30%, 2 <15%, 3 charging

    // DEBUG: force sleepy demo regardless of battery (set true only for demo)
    private val demoForceSleepy: Boolean = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private val serviceHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                GlyphToy.MSG_GLYPH_TOY -> {
                    val bundle = msg.data
                    val event = bundle.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
                    when (event) {
                        GlyphToy.EVENT_CHANGE -> triggerSurprise()
                        GlyphToy.EVENT_ACTION_DOWN -> handleButtonDown()
                        GlyphToy.EVENT_ACTION_UP -> handleButtonUp()
                        GlyphToy.EVENT_AOD -> {
                            handleAodTick()
                            // Randomly trigger different emotions during AOD
                            val random = (0..100).random()
                            when {
                                random < 5 -> triggerSleepy()
                                random < 10 -> triggerAngry()
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
        gm = GlyphMatrixManager.getInstance(this)
        gm?.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(name: android.content.ComponentName?) {}
            override fun onServiceDisconnected(name: android.content.ComponentName?) {}
        })
        gm?.register(Glyph.DEVICE_23112)

        frameBuilder = GlyphMatrixFrame.Builder()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }

        // Register battery receiver
        registerBatteryReceiver()

        if (demoForceSleepy) {
            batteryMoodTier = 2
            batterySleepBias = 0.6f
            triggerSleepy()
        }

        startFrameLoop()

        // 起動直後はデモパターンから開始
        scheduleNextDemoMotion()
    }

    private fun teardown() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        accelerometer = null
        mainHandler.removeCallbacksAndMessages(null)
        // Unregister battery receiver
        try {
            batteryReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) {}
        batteryReceiver = null
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
        animate(
            150L,
            onUpdate = { t ->
                blinkProgress = if (t < 0.5f) t * 2f else (1f - (t - 0.5f) * 2f)
            },
            onEnd = {
                isBlinking = false
                blinkProgress = 0f
            }
        )
    }

    private fun triggerSurprise() {
        if (isSurprised) return
        isSurprised = true
        surpriseProgress = 0f
        animate(
            300L,
            onUpdate = { t ->
                surpriseProgress = if (t < 0.5f) t * 2f else (1f - (t - 0.5f) * 2f)
            },
            onEnd = {
                isSurprised = false
                surpriseProgress = 0f
            }
        )
    }
    
    private fun triggerSleepy() {
        if (isSleepy) return
        isSleepy = true
        sleepProgress = 0f
        animate(
            2000L, // longer duration for sleepy state
            onUpdate = { t ->
                sleepProgress = if (t < 0.3f) t / 0.3f else 1f
            },
            onEnd = {
                isSleepy = false
                sleepProgress = 0f
            }
        )
    }
    
    private fun triggerAngry() {
        if (isAngry) return
        isAngry = true
        angryProgress = 0f
        animate(
            800L,
            onUpdate = { t ->
                angryProgress = if (t < 0.5f) t * 2f else (1f - (t - 0.5f) * 2f)
            },
            onEnd = {
                isAngry = false
                angryProgress = 0f
            }
        )
    }
    
    private fun triggerSquint() {
        if (isSquinting) return
        isSquinting = true
        squintProgress = 0f
        animate(
            800L,
            onUpdate = { t ->
                squintProgress = if (t < 0.5f) t * 2f else (1f - (t - 0.5f) * 2f)
            },
            onEnd = {
                isSquinting = false
                squintProgress = 0f
            }
        )
    }

    private fun handleAodTick() {
        val now = SystemClock.uptimeMillis()
        
        // 1分間隔でランダムな目位置に変更
        if (now - aodLastChangeAt >= aodIntervalMs) {
            aodLastChangeAt = now
            generateRandomAodPosition()
        }
        
        // AOD用の固定位置を適用
        filteredX = aodOffsetX
        filteredY = aodOffsetY
    }

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
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL || plugged != 0
                // Decide tier
                val newTier = when {
                    isCharging -> 3
                    percent < 15 -> 2
                    percent < 30 -> 1
                    else -> 0
                }
                if (newTier != batteryMoodTier) {
                    batteryMoodTier = newTier
                    when (batteryMoodTier) {
                        2 -> { // strong sleepy
                            batterySleepBias = 0.6f
                            triggerSleepy()
                        }
                        1 -> { // mild sleepy
                            batterySleepBias = 0.3f
                            triggerSleepy()
                        }
                        3 -> { // charging -> wake up
                            batterySleepBias = 0f
                            triggerSurprise()
                        }
                        else -> { // normal
                            batterySleepBias = 0f
                        }
                    }
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        // Register and also immediately handle sticky intent result
        val sticky = registerReceiver(batteryReceiver, filter)
        sticky?.let { batteryReceiver?.onReceive(this, it) }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val ax = event.values[0]
        val ay = event.values[1]
        // Fix X-axis: when device is tilted right (ax positive), pupils should go right (nx positive)
        val nx = (ax / 3f).coerceIn(-1f, 1f) // increased sensitivity
        // Fix Y-axis: when device is tilted up (ay negative), pupils should go up (ny negative)
        val ny = (ay / 3f).coerceIn(-1f, 1f) // increased sensitivity
        filteredX += alpha * (nx - filteredX)
        filteredY += alpha * (ny - filteredY)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun renderFrame() {
        val builder = GlyphMatrixFrame.Builder()

        // Calculate eye size based on emotions
        val scaleBoost = 1f + 0.3f * surpriseProgress
        val effectiveSleep = (sleepProgress + batterySleepBias).coerceIn(0f, 1f)
        // Sleep should only affect eyelids, not eyeball size
        val sleepScale = 1f
        val angryScale = 1f + 0.2f * angryProgress // eyes get slightly bigger when angry
        val squintScale = 1f - 0.6f * squintProgress // eyes get much smaller when squinting
        
        val finalScale = scaleBoost * angryScale * squintScale
        val eyeRX = (eyeRadiusX * finalScale).roundToInt().coerceAtLeast(1)
        val eyeRY = (eyeRadiusY * finalScale).roundToInt().coerceAtLeast(1)

        // 瞳オフセット（モード別）
        val (lx, ly, rx, ry) = when (eyeMode) {
            EyeMode.SENSOR -> {
                val ox = (filteredX * pupilRange).roundToInt()
                val oy = (filteredY * pupilRange).roundToInt()
                val (lox, loy) = applyDirectionLimits(ox, oy)
                val (rox, roy) = applyDirectionLimits(ox, oy)
                Quad(lox, loy, rox, roy)
            }
            EyeMode.DEMO -> computeDemoOffsets()
        }

        val leftPupilCenter = Pair(eyeCenterLeft.first + lx, eyeCenterLeft.second + ly)
        val rightPupilCenter = Pair(eyeCenterRight.first + rx, eyeCenterRight.second + ry)

        // 目のリングと瞳を1枚のビットマップに描画
        val bm = drawEyesBitmap(
            width = 25,
            height = 25,
            leftEyeCenter = eyeCenterLeft,
            rightEyeCenter = eyeCenterRight,
            eyeRadiusX = eyeRX,
            eyeRadiusY = eyeRY,
            outline = 2,
            pupilLeftCenter = leftPupilCenter,
            pupilRightCenter = rightPupilCenter,
            pupilRadius = pupilRadius,
            blink = blinkProgress,
            sleep = effectiveSleep,
            angry = angryProgress,
            squint = squintProgress
        )

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

        // 25x25 のビットマップに「接続した縦楕円の白目＋黒縁」「瞳（左下寄り）＋細長い三日月ハイライト」「まぶた（blink）」を描く
    private fun drawEyesBitmap(
        width: Int,
        height: Int,
        leftEyeCenter: Pair<Int, Int>,
        rightEyeCenter: Pair<Int, Int>,
        eyeRadiusX: Int,
        eyeRadiusY: Int,
        outline: Int,
        pupilLeftCenter: Pair<Int, Int>,
        pupilRightCenter: Pair<Int, Int>,
        pupilRadius: Int,
        blink: Float,
        sleep: Float = 0f,
        angry: Float = 0f,
        squint: Float = 0f
    ): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // 背景は黒（消灯）
        canvas.drawColor(Color.BLACK)

        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = outline.toFloat()
        }
        val blackFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        val highlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        // 接続した縦楕円の白目を描画
        fun drawConnectedEyes() {
            // 左目（縦楕円）
            canvas.drawOval(
                (leftEyeCenter.first - eyeRadiusX).toFloat(),
                (leftEyeCenter.second - eyeRadiusY).toFloat(),
                (leftEyeCenter.first + eyeRadiusX).toFloat(),
                (leftEyeCenter.second + eyeRadiusY).toFloat(),
                whitePaint
            )
            // 右目（縦楕円）
            canvas.drawOval(
                (rightEyeCenter.first - eyeRadiusX).toFloat(),
                (rightEyeCenter.second - eyeRadiusY).toFloat(),
                (rightEyeCenter.first + eyeRadiusX).toFloat(),
                (rightEyeCenter.second + eyeRadiusY).toFloat(),
                whitePaint
            )
            // 中央の接続部分（矩形で繋ぐ）
            val centerY = leftEyeCenter.second.toFloat()
            val connectionWidth = (rightEyeCenter.first - leftEyeCenter.first - eyeRadiusX * 2).toFloat()
            if (connectionWidth > 0) {
                canvas.drawRect(
                    (leftEyeCenter.first + eyeRadiusX).toFloat(),
                    (centerY - eyeRadiusY / 2f),
                    (rightEyeCenter.first - eyeRadiusX).toFloat(),
                    (centerY + eyeRadiusY / 2f),
                    whitePaint
                )
            }
        }

        // 黒縁を描画
        fun drawOutline() {
            // 左目の縁（縦楕円）
            canvas.drawOval(
                (leftEyeCenter.first - eyeRadiusX).toFloat(),
                (leftEyeCenter.second - eyeRadiusY).toFloat(),
                (leftEyeCenter.first + eyeRadiusX).toFloat(),
                (leftEyeCenter.second + eyeRadiusY).toFloat(),
                strokePaint
            )
            // 右目の縁（縦楕円）
            canvas.drawOval(
                (rightEyeCenter.first - eyeRadiusX).toFloat(),
                (rightEyeCenter.second - eyeRadiusY).toFloat(),
                (rightEyeCenter.first + eyeRadiusX).toFloat(),
                (rightEyeCenter.second + eyeRadiusY).toFloat(),
                strokePaint
            )
            // 接続部分の縁
            val centerY = leftEyeCenter.second.toFloat()
            val connectionWidth = (rightEyeCenter.first - leftEyeCenter.first - eyeRadiusX * 2).toFloat()
            if (connectionWidth > 0) {
                val strokeRect = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    style = Paint.Style.STROKE
                    strokeWidth = outline.toFloat()
                }
                canvas.drawRect(
                    (leftEyeCenter.first + eyeRadiusX).toFloat(),
                    (centerY - eyeRadiusY / 2f),
                    (rightEyeCenter.first - eyeRadiusX).toFloat(),
                    (centerY + eyeRadiusY / 2f),
                    strokeRect
                )
            }
        }

        // 瞳とハイライトを描画（瞳は左下寄り、ハイライトは細長い三日月形）
        fun drawPupilAndHighlight(eyeCenterX: Int, eyeCenterY: Int, pupilCx: Int, pupilCy: Int) {
            // 瞳（左下寄りに配置）
            val pupilOffsetX = -pupilRadius / 2 // 左寄り
            val pupilOffsetY = pupilRadius / 2  // 下寄り
            val finalPupilX = pupilCx + pupilOffsetX
            val finalPupilY = pupilCy + pupilOffsetY
            canvas.drawCircle(finalPupilX.toFloat(), finalPupilY.toFloat(), pupilRadius.toFloat(), blackFill)
            
            // 細長い三日月形のハイライト（瞳の右上端）
            val hlX = finalPupilX + pupilRadius * 0.6f
            val hlY = finalPupilY - pupilRadius * 0.6f
            val hlWidth = pupilRadius * 1.2f  // より細長く
            val hlHeight = pupilRadius * 0.2f // より薄く
            
            // 細長い楕円形のハイライト
            canvas.save()
            canvas.translate(hlX, hlY)
            canvas.rotate(25f) // 右斜め上向き
            canvas.drawOval(-hlWidth/2f, -hlHeight/2f, hlWidth/2f, hlHeight/2f, highlight)
            canvas.restore()
        }

        // 瞳のはみ出し防止（楕円内にクランプ・上下で許容量を変える）
        fun clamp(center: Pair<Int, Int>, target: Pair<Int, Int>): Pair<Int, Int> {
            val dx = (target.first - center.first).toFloat()
            val dy = (target.second - center.second).toFloat()
            // 横は瞳半径ぶんだけ内側に。縦は上方向(+2px猶予)、下方向(-1px厳しめ)
            val safeRadiusX = (eyeRadiusX - pupilRadius).coerceAtLeast(1)
            val safeRadiusYUp = (eyeRadiusY - pupilRadius + 2).coerceAtLeast(1)    // 上に+2px伸ばす
            val safeRadiusYDown = (eyeRadiusY - pupilRadius - 1).coerceAtLeast(1)   // 下はやや厳しめ

            val ry = if (dy < 0f) safeRadiusYUp.toFloat() else safeRadiusYDown.toFloat()
            val safeRadiusXRight = (eyeRadiusX - pupilRadius + 1).coerceAtLeast(1)
            // 左方向は黒縁に潜り込まないように1px手前で止める
            val safeRadiusXLeft = (eyeRadiusX - pupilRadius - 1).coerceAtLeast(1)
            val rx = (if (dx > 0f) safeRadiusXRight else safeRadiusXLeft).toFloat()

            val ellipseDist = (dx * dx) / (rx * rx) + (dy * dy) / (ry * ry)
            if (ellipseDist <= 1f) return target
            // 楕円の境界上にクランプ（上下で半径を切り替え）
            val angle = atan2(dy, dx)
            val useRy = if (sin(angle) < 0f) safeRadiusYUp.toFloat() else safeRadiusYDown.toFloat()
            val clampedX = (center.first + rx * cos(angle)).roundToInt()
            val clampedY = (center.second + useRy * sin(angle)).roundToInt()
            return Pair(clampedX, clampedY)
        }

        // Apply ellipse clamping to prevent overflow
        val clL = clamp(leftEyeCenter, pupilLeftCenter)
        val clR = clamp(rightEyeCenter, pupilRightCenter)

        // 描画順序
        drawConnectedEyes()
        drawOutline()
        drawPupilAndHighlight(leftEyeCenter.first, leftEyeCenter.second, clL.first, clL.second)
        drawPupilAndHighlight(rightEyeCenter.first, rightEyeCenter.second, clR.first, clR.second)

        // まばたき（上下から黒バー）
        if (blink > 0f) {
            val cover = (eyeRadiusY * blink).roundToInt().coerceAtLeast(1)
            val topY = max(0, leftEyeCenter.second - eyeRadiusY)
            val botY = min(height, leftEyeCenter.second + eyeRadiusY)
            canvas.drawRect(0f, topY.toFloat(), width.toFloat(), (topY + cover).toFloat(), blackFill)
            canvas.drawRect(0f, (botY - cover).toFloat(), width.toFloat(), botY.toFloat(), blackFill)
        }
        
        // 眠い状態（上のまぶたが重く降りる）
        if (sleep > 0f) {
            val sleepCover = (eyeRadiusY * sleep * 0.9f).roundToInt().coerceAtLeast(1)
            val topY = max(0, leftEyeCenter.second - eyeRadiusY)
            canvas.drawRect(0f, topY.toFloat(), width.toFloat(), (topY + sleepCover).toFloat(), blackFill)
        }
        
        // 怒った状態（眉毛のような線を追加）
        if (angry > 0f) {
            val angryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            // 左目の上に怒った眉毛
            val leftEyebrowY = leftEyeCenter.second - eyeRadiusY - 2
            canvas.drawLine(
                (leftEyeCenter.first - eyeRadiusX + 1).toFloat(),
                leftEyebrowY.toFloat(),
                (leftEyeCenter.first + eyeRadiusX - 1).toFloat(),
                (leftEyebrowY - 2 * angry).toFloat(),
                angryPaint
            )
            // 右目の上に怒った眉毛
            val rightEyebrowY = rightEyeCenter.second - eyeRadiusY - 2
            canvas.drawLine(
                (rightEyeCenter.first - eyeRadiusX + 1).toFloat(),
                (rightEyebrowY - 2 * angry).toFloat(),
                (rightEyeCenter.first + eyeRadiusX - 1).toFloat(),
                rightEyebrowY.toFloat(),
                angryPaint
            )
        }
        
        // 目が笑う状態（目を細くする）
        if (squint > 0f) {
            // 目を細くするために上下から黒いバーで覆う
            val squintCover = (eyeRadiusY * squint * 0.8f).roundToInt().coerceAtLeast(1)
            val topY = max(0, leftEyeCenter.second - eyeRadiusY)
            val botY = min(height, leftEyeCenter.second + eyeRadiusY)
            canvas.drawRect(0f, topY.toFloat(), width.toFloat(), (topY + squintCover).toFloat(), blackFill)
            canvas.drawRect(0f, (botY - squintCover).toFloat(), width.toFloat(), botY.toFloat(), blackFill)
        }

        return bmp
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

    // ====== 追加: ボタン/モード/デモ ======
    private fun handleButtonDown() {
        lastActionDownAt = SystemClock.uptimeMillis()
    }

    private fun handleButtonUp() {
        val now = SystemClock.uptimeMillis()
        val dur = now - lastActionDownAt
        if (dur >= longPressThresholdMs) {
            // 長押し: 2回まばたき → モードをトグル
            val target = if (eyeMode == EyeMode.DEMO) EyeMode.SENSOR else EyeMode.DEMO
            triggerDoubleBlinkThenSwitch(target)
        } else {
            // 短押し: 軽い表情
            triggerSquint()
        }
    }

    private fun triggerDoubleBlinkThenSwitch(target: EyeMode) {
        // 一回目
        triggerBlink()
        // 少し待って二回目
        mainHandler.postDelayed({ triggerBlink() }, 240L)
        // さらに待ってモード切替
        mainHandler.postDelayed({
            eyeMode = target
            if (target == EyeMode.DEMO) {
                // デモに戻るときはパターンをリセット
                scheduleNextDemoMotion()
            }
        }, 520L)
    }

    // 方向ごとの上限を適用
    private fun applyDirectionLimits(dx: Int, dy: Int): Pair<Int, Int> {
        val limitedX = when {
            dx > 0 -> minOf(dx, 4)
            dx < 0 -> maxOf(dx, -2)
            else -> dx
        }
        val limitedY = when {
            dy > 0 -> minOf(dy, 2)
            dy < 0 -> maxOf(dy, -5)
            else -> dy
        }
        return Pair(limitedX, limitedY)
    }

    private data class Quad(val lx: Int, val ly: Int, val rx: Int, val ry: Int)

    private fun scheduleNextDemoMotion() {
        val now = SystemClock.uptimeMillis()
        demoStartAt = now
        // 1.5〜4.0秒のゆっくり区間
        demoDurationMs = (1500L..4000L).random()
        // 重み付け:
        //  - STOP: 約10%
        //  - 残りは「同方向(Left-Right/Up-Down) : 左右バラバラ(Cross/Apart/Drift) ≈ 3 : 1」
        val r = (0..99).random()
        demoType = if (r < 10) {
            DemoType.STOP
        } else {
            val r2 = (0..99).random()
            if (r2 < 75) {
                // 同方向（3/4）
                if ((0..1).random() == 0) DemoType.LR else DemoType.UD
            } else {
                // 左右バラバラ（1/4）
                listOf(DemoType.CROSSEYE, DemoType.APART, DemoType.DRIFT).random()
            }
        }
        demoSeed = (0..10000).random() / 1000f
    }

    private fun computeDemoOffsets(): Quad {
        val now = SystemClock.uptimeMillis()
        if (now - demoStartAt >= demoDurationMs) {
            scheduleNextDemoMotion()
        }
        val t = (now - demoStartAt).toFloat() / demoDurationMs.toFloat()
        val twoPi = (PI * 2).toFloat()
        val s = sin(twoPi * t).toFloat()

        val rangeX = 4 // 横の最大レンジ
        val rangeY = 2 // 縦の最大レンジ

        return when (demoType) {
            DemoType.LR -> {
                val x = (s * rangeX).roundToInt()
                val (lx, ly) = applyDirectionLimits(x, 0)
                val (rx, ry) = applyDirectionLimits(x, 0)
                Quad(lx, ly, rx, ry)
            }
            DemoType.UD -> {
                val y = (s * rangeY).roundToInt()
                val (lx, ly) = applyDirectionLimits(0, y)
                val (rx, ry) = applyDirectionLimits(0, y)
                Quad(lx, ly, rx, ry)
            }
            DemoType.CROSSEYE -> {
                val base = (abs(s) * rangeX).roundToInt()
                val (lx, ly) = applyDirectionLimits(+base, 0)
                val (rx, ry) = applyDirectionLimits(-base, 0)
                Quad(lx, ly, rx, ry)
            }
            DemoType.APART -> {
                val base = (abs(s) * rangeX).roundToInt()
                val (lx, ly) = applyDirectionLimits(-base, 0)
                val (rx, ry) = applyDirectionLimits(+base, 0)
                Quad(lx, ly, rx, ry)
            }
            DemoType.DRIFT -> {
                // 低速・低振幅の左右独立ドリフト
                val phase = twoPi * t
                val xL = (sin(phase * 0.6f + demoSeed) * 2f).roundToInt()
                val yL = (cos(phase * 0.4f + demoSeed) * 1f).roundToInt()
                val xR = (sin(phase * 0.6f + demoSeed + PI.toFloat()) * 2f).roundToInt()
                val yR = (cos(phase * 0.4f + demoSeed + PI.toFloat()) * 1f).roundToInt()
                val (lx, ly) = applyDirectionLimits(xL, yL)
                val (rx, ry) = applyDirectionLimits(xR, yR)
                Quad(lx, ly, rx, ry)
            }
            DemoType.STOP -> {
                Quad(0, 0, 0, 0)
            }
        }
    }

    // AOD用ランダム位置生成
    private fun generateRandomAodPosition() {
        // 通常のデモパターンと同じ種類をランダム選択
        val patterns = listOf("LR", "UD", "CROSSEYE", "APART", "DRIFT", "STOP")
        val pattern = patterns[(0 until patterns.size).random()]
        
        when (pattern) {
            "LR" -> {
                // 左右どちらかの端へ
                val direction = if ((0..1).random() == 0) -1f else 1f
                aodOffsetX = direction * 0.8f // やや控えめ
                aodOffsetY = 0f
            }
            "UD" -> {
                // 上下どちらかへ
                val direction = if ((0..1).random() == 0) -1f else 1f
                aodOffsetX = 0f
                aodOffsetY = direction * 0.5f // 縦方向は控えめ
            }
            "CROSSEYE" -> {
                // 寄り目（左右で逆方向）- ここでは右寄り設定
                aodOffsetX = 0.6f
                aodOffsetY = 0f
            }
            "APART" -> {
                // 離れ目 - ここでは左寄り設定
                aodOffsetX = -0.4f
                aodOffsetY = 0f
            }
            "DRIFT" -> {
                // ランダムな位置
                aodOffsetX = ((-8..8).random() / 10f)
                aodOffsetY = ((-4..4).random() / 10f)
            }
            "STOP" -> {
                // 中央
                aodOffsetX = 0f
                aodOffsetY = 0f
            }
        }
    }
}


