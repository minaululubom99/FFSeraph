package com.freefire.seraph

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.FrameLayout
import kotlin.math.hypot
import kotlin.random.Random

class HackService : AccessibilityService() {

    private lateinit var wm: WindowManager
    private var overlay: FrameLayout? = null
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private var aimX = 0f
    private var aimY = 0f
    private var targetVisible = false
    private var loopCounter = 0
    private var sleepCounter = 0
    private val random = Random(123)

    override fun onServiceConnected() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlay()
        isRunning = true
        startLoop()
    }

    private fun createOverlay() {
        val params = LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                LayoutParams.TYPE_APPLICATION_OVERLAY
            else LayoutParams.TYPE_PHONE,
            LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        overlay = FrameLayout(this).apply {
            val view = object : android.view.View(this@HackService) {
                val paint = Paint().apply {
                    color = Color.GREEN
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                val fill = Paint().apply { color = Color.argb(80, 0, 255, 0) }
                override fun onDraw(canvas: Canvas) {
                    super.onDraw(canvas)
                    val cx = width / 2f
                    val cy = height / 2f
                    canvas.drawCircle(cx, cy, 250f, paint)
                    if (targetVisible) {
                        canvas.drawCircle(aimX, aimY, 35f, fill)
                    }
                }
            }
            addView(view, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        wm.addView(overlay, params)
    }

    private fun startLoop() {
        handler.post(object : Runnable {
            override fun run() {
                if (!isRunning) return
                loopCounter++
                if (loopCounter % 300 == 0) {
                    sleepCounter = random.nextInt(15, 30) * 16
                }
                if (sleepCounter > 0) {
                    sleepCounter--
                } else {
                    val centerX = 540f
                    val centerY = 960f
                    aimX = centerX + random.nextFloat() * 200 - 100
                    aimY = centerY + random.nextFloat() * 200 - 100
                    val dist = hypot((aimX - centerX).toDouble(), (aimY - centerY).toDouble())
                    targetVisible = dist <= 250
                    if (targetVisible) {
                        performShoot(aimX, aimY)
                    }
                }
                overlay?.invalidate()
                val delay = (16 + random.nextInt(5, 15)).toLong()
                handler.postDelayed(this, delay)
            }
        })
    }

    private fun performShoot(x: Float, y: Float) {
        val offX = (random.nextFloat() * 5 + 2) * if (random.nextBoolean()) 1f else -1f
        val offY = (random.nextFloat() * 5 + 2) * if (random.nextBoolean()) 1f else -1f
        val tapX = x + offX
        val tapY = y + offY
        val duration = random.nextInt(50, 150).toLong()
        val path = Path()
        path.moveTo(tapX, tapY)
        if (random.nextBoolean()) {
            path.lineTo(tapX + random.nextFloat() * 10 - 5, tapY + random.nextFloat() * 10 - 5)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        val builder = GestureDescription.Builder().addStroke(stroke)
        dispatchGesture(builder.build(), null, null)
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        overlay?.let { wm.removeView(it) }
        super.onDestroy()
    }

    override fun onInterrupt() {}
}
