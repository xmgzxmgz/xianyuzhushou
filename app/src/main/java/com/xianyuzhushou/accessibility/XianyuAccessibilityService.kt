package com.xianyuzhushou.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.xianyuzhushou.ui.MainActivity

class XianyuAccessibilityService : AccessibilityService() {

    private val TAG = "XyService"
    private var running = false
    private var coinCount = 0
    private lateinit var manager: AutomationManager

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MainActivity.ACTION_START -> startAutomation()
                MainActivity.ACTION_STOP -> stopAutomation()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        manager = AutomationManager(this)
        LocalBroadcastManager.getInstance(this).registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(MainActivity.ACTION_START)
                addAction(MainActivity.ACTION_STOP)
            }
        )
        Log.d(TAG, "服务已连接")
        broadcastStatus("服务已连接，等待开始")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!running) return
        try {
            val root = rootInActiveWindow ?: return
            // 执行一次循环，若无操作则稍作等待
            val acted = manager.performOneCycle(root)
            if (!acted) Thread.sleep(500)
        } catch (e: Throwable) {
            Log.e(TAG, "onAccessibilityEvent error", e)
            broadcastStatus("异常：${e.message}")
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "服务被中断")
        running = false
        broadcastStatus("服务中断")
    }

    private fun startAutomation() {
        if (running) return
        running = true
        broadcastStatus("运行中…请切到闲鱼任务页")
    }

    private fun stopAutomation() {
        if (!running) return
        running = false
        broadcastStatus("已停止")
    }

    fun onRewardClaimed(coin: Int) {
        coinCount += coin
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent(MainActivity.ACTION_UPDATE_COIN).putExtra("coin", coinCount)
        )
    }

    private fun broadcastStatus(msg: String) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent(MainActivity.ACTION_UPDATE_STATUS).putExtra("status", msg)
        )
    }
}