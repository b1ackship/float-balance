package com.eta.floatbalance

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.concurrent.thread

class FloatWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatView: View? = null
    private var balanceText: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: Prefs

    private val refreshRunnable = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 5 * 60 * 1000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = Prefs(this)
        startForeground(1, buildNotification())
        createFloatView()
        refresh()
        handler.postDelayed(refreshRunnable, 5 * 60 * 1000L)
    }

    private fun buildNotification(): Notification {
        val channelId = "float_balance"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(channelId, "余额悬浮窗", NotificationManager.IMPORTANCE_LOW))
        }
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("DeepSeek 余额")
            .setContentText("悬浮窗运行中")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pi)
            .build()
    }

    private fun createFloatView() {
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xDD000000.toInt())
            setPadding(36, 24, 36, 24)
        }
        view.addView(TextView(this).apply {
            text = "DeepSeek 余额"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 11f
        })
        val value = TextView(this).apply {
            text = "加载中..."
            setTextColor(0xFF4ADE80.toInt())
            textSize = 18f
        }
        view.addView(value)
        balanceText = value

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300
        }

        var startX = 0f
        var startY = 0f
        var touchX = 0f
        var touchY = 0f
        var isDrag = false
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    startY = event.rawY
                    touchX = params.x.toFloat()
                    touchY = params.y.toFloat()
                    isDrag = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startX
                    val dy = event.rawY - startY
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) isDrag = true
                    if (isDrag) {
                        params.x = (touchX + dx).toInt()
                        params.y = (touchY + dy).toInt()
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDrag) refresh()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(view, params)
        floatView = view
    }

    private fun refresh() {
        val key = prefs.apiKey
        if (key.isBlank()) {
            balanceText?.text = "未设置 Key"
            return
        }
        thread {
            val result = try {
                BalanceFetcher.fetch(key)
            } catch (e: Exception) {
                "请求失败"
            }
            handler.post { balanceText?.text = result }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        floatView?.let { runCatching { windowManager.removeView(it) } }
        floatView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
