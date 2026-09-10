package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import com.example.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingBubbleService : Service() {

    companion object {
        var isRunning = false
            private set
    }

    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var observerJob: Job? = null

    private lateinit var tvDownload: TextView
    private lateinit var tvUpload: TextView

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        initFloatingBubble()
        startObservingSpeed()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initFloatingBubble() {
        val wm = windowManager ?: return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 200
        }

        // Build root container
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))

            val bg = GradientDrawable().apply {
                setColor(0xE60F172A.toInt()) // Deep Dark Glass
                cornerRadius = dpToPx(24).toFloat()
                setStroke(dpToPx(1), 0x4038BDF8.toInt()) // Cyan border
            }
            background = bg
            elevation = dpToPx(8).toFloat()
        }

        // Speed text container
        val speedLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        tvDownload = TextView(this).apply {
            text = "↓ 0.0 KB/s"
            setTextColor(0xFF34D399.toInt()) // Emerald Green
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
        }

        tvUpload = TextView(this).apply {
            text = "↑ 0.0 KB/s"
            setTextColor(0xFF38BDF8.toInt()) // Sky Blue
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
        }

        speedLayout.addView(tvDownload)
        speedLayout.addView(tvUpload)
        rootLayout.addView(speedLayout)

        // Close button
        val btnClose = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(0x80FFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(dpToPx(16), dpToPx(16)).apply {
                marginStart = dpToPx(8)
            }
            setOnClickListener {
                stopSelf()
            }
        }
        rootLayout.addView(btnClose)

        // Drag and click handling
        rootLayout.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = false

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event == null) return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isClick = false
                        }
                        params.x = initialX + dx
                        params.y = initialY + dy
                        wm.updateViewLayout(rootLayout, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            // Tap opens main activity
                            val intent = Intent(this@FloatingBubbleService, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            startActivity(intent)
                        }
                        return true
                    }
                }
                return false
            }
        })

        bubbleView = rootLayout
        wm.addView(rootLayout, params)
    }

    private fun startObservingSpeed() {
        observerJob = scope.launch {
            NetworkSpeedService.currentSpeed.collectLatest { speed ->
                tvDownload.text = "↓ ${NetworkUtils.formatSpeed(speed.downloadBps)}"
                tvUpload.text = "↑ ${NetworkUtils.formatSpeed(speed.uploadBps)}"
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        observerJob?.cancel()
        bubbleView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                // Ignore
            }
        }
        bubbleView = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
