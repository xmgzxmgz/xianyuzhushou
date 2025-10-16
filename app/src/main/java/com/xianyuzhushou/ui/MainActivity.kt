package com.xianyuzhushou.ui

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvDailyCoin: TextView
    private lateinit var tvTotalCoin: TextView
    private lateinit var tvLog: TextView
    private lateinit var lbm: LocalBroadcastManager
    private val uiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: android.content.Intent?) {
            when (intent?.action) {
                ACTION_UPDATE_STATUS -> {
                    val s = intent.getStringExtra("status") ?: ""
                    tvStatus.text = "状态：$s"
                    appendLog(s)
                }
                ACTION_UPDATE_STATS -> {
                    val daily = intent.getIntExtra("daily", 0)
                    val total = intent.getIntExtra("total", 0)
                    tvDailyCoin.text = "今日闲鱼币：$daily"
                    tvTotalCoin.text = "累计闲鱼币：$total"
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.xianyuzhushou.R.layout.activity_main)

        tvStatus = findViewById(com.xianyuzhushou.R.id.tvStatus)
        tvDailyCoin = findViewById(com.xianyuzhushou.R.id.tvDailyCoin)
        tvTotalCoin = findViewById(com.xianyuzhushou.R.id.tvTotalCoin)
        tvLog = findViewById(com.xianyuzhushou.R.id.tvLog)
        tvLog.movementMethod = ScrollingMovementMethod()
        lbm = LocalBroadcastManager.getInstance(this)
        lbm.registerReceiver(uiReceiver, IntentFilter().apply {
            addAction(ACTION_UPDATE_STATUS)
            addAction(ACTION_UPDATE_STATS)
        })

        findViewById<Button>(com.xianyuzhushou.R.id.btnOpenAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<Button>(com.xianyuzhushou.R.id.btnStart).setOnClickListener {
            sendActionBroadcast(ACTION_START)
            appendLog("已发送开始任务指令")
            tvStatus.text = "状态：运行中"
        }

        findViewById<Button>(com.xianyuzhushou.R.id.btnStop).setOnClickListener {
            sendActionBroadcast(ACTION_STOP)
            appendLog("已发送停止任务指令")
            tvStatus.text = "状态：已停止"
        }

        // 打开闲鱼应用任务页(可选)：通过深链或普通启动
        // 这里只提供提示，具体页面导航由用户手动或服务尝试跳转
        appendLog("请打开闲鱼APP的任务中心页面以便自动化")
    }

    private fun sendActionBroadcast(action: String) {
        val intent = Intent(action)
        lbm.sendBroadcast(intent)
    }

    private fun appendLog(msg: String) {
        tvLog.append("\n$msg")
        tvLog.post {
            val layout = tvLog.layout ?: return@post
            val lastLineTop = layout.getLineTop(tvLog.lineCount - 1)
            val scrollAmount = lastLineTop - tvLog.height
            if (scrollAmount > 0) tvLog.scrollTo(0, scrollAmount) else tvLog.scrollTo(0, 0)
        }
    }

    companion object {
        const val ACTION_START = "com.xianyuzhushou.ACTION_START"
        const val ACTION_STOP = "com.xianyuzhushou.ACTION_STOP"
        const val ACTION_UPDATE_STATUS = "com.xianyuzhushou.ACTION_UPDATE_STATUS"
        const val ACTION_UPDATE_STATS = "com.xianyuzhushou.ACTION_UPDATE_STATS"
    }
}