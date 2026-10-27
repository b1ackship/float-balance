package com.eta.floatbalance

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        val dp = resources.displayMetrics.density
        val pad = (12 * dp).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(pad, pad * 3, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "DeepSeek 余额悬浮窗"
            textSize = 20f
        })

        root.addView(TextView(this).apply {
            text = "API Key（sk- 开头）"
            textSize = 13f
            setPadding(0, pad * 2, 0, pad)
        })

        val keyInput = EditText(this).apply {
            setText(prefs.apiKey)
            hint = "sk-..."
            isSingleLine = true
        }
        root.addView(keyInput)

        root.addView(Button(this).apply {
            text = "保存 Key"
            setOnClickListener {
                val k = keyInput.text.toString().trim()
                if (k.startsWith("sk-")) {
                    prefs.apiKey = k
                    toast("已保存")
                } else {
                    toast("请输入以 sk- 开头的 Key")
                }
            }
        })

        root.addView(Button(this).apply {
            text = "显示 / 刷新悬浮窗"
            setOnClickListener {
                if (prefs.apiKey.isBlank()) {
                    toast("请先保存 API Key")
                    return@setOnClickListener
                }
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")))
                    toast("请授权「显示在其他应用上层」后重试")
                    return@setOnClickListener
                }
                startService(Intent(this@MainActivity, FloatWindowService::class.java))
            }
        })

        root.addView(Button(this).apply {
            text = "关闭悬浮窗"
            setOnClickListener {
                stopService(Intent(this@MainActivity, FloatWindowService::class.java))
            }
        })

        setContentView(root)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
