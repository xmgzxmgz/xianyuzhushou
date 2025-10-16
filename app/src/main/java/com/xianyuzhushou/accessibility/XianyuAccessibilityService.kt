package com.xianyuzhushou.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.xianyuzhushou.ui.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class XianyuAccessibilityService : AccessibilityService() {

    private val TAG = "XyService"
    private var running = false
    private var dailyCoin = 0
    private var totalCoin = 0
    private var dailyDate: String = ""
    private lateinit var prefs: SharedPreferences
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private lateinit var overlay: OverlayController
    private var didDailySignIn = false
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
        prefs = getSharedPreferences("xy_prefs", Context.MODE_PRIVATE)
        overlay = OverlayController(this)
        // 加载统计并进行日期校验
        dailyCoin = prefs.getInt("dailyCoin", 0)
        totalCoin = prefs.getInt("totalCoin", 0)
        dailyDate = prefs.getString("dailyDate", "") ?: ""
        val today = dateFmt.format(Date())
        if (dailyDate != today) {
            dailyDate = today
            dailyCoin = 0
            prefs.edit().putString("dailyDate", dailyDate).putInt("dailyCoin", dailyCoin).apply()
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(MainActivity.ACTION_START)
                addAction(MainActivity.ACTION_STOP)
            }
        )
        Log.d(TAG, "服务已连接")
        broadcastStatus("服务已连接，等待开始")
        broadcastStats()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!running) return
        try {
            val root = rootInActiveWindow ?: return
            // 执行一次循环，若无操作则稍作等待
            // 先尝试签到（仅一次）
            if (!didDailySignIn) {
                val signed = manager.tryDailySignIn(root)
                if (signed) {
                    didDailySignIn = true
                    log("签到完成（或提示明日再来）")
                }
            }
            val acted = manager.performOneCycle(root)
            // 任务完成提示检测（非严格，仅文案匹配）
            if (manager.checkTaskCompleted(root)) {
                log("检测到任务已完成或已领取标识")
            }
            if (!acted) Thread.sleep(500)
        } catch (e: Throwable) {
            Log.e(TAG, "onAccessibilityEvent error", e)
            log("异常：${e.message}")
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
        overlay.resetClosedFlag()
        overlay.show()
        log("运行中…请切到闲鱼任务页")
    }

    private fun stopAutomation() {
        if (!running) return
        running = false
        overlay.hide()
        broadcastStatus("已停止")
    }

    fun onRewardClaimed(coinGain: Int) {
        if (coinGain <= 0) return
        dailyCoin += coinGain
        totalCoin += coinGain
        prefs.edit().putInt("dailyCoin", dailyCoin).putInt("totalCoin", totalCoin).apply()
        broadcastStats()
        log("奖励到账 +${coinGain} 币（今日:${dailyCoin} 累计:${totalCoin}）")
    }

    private fun broadcastStatus(msg: String) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent(MainActivity.ACTION_UPDATE_STATUS).putExtra("status", msg)
        )
    }

    private fun broadcastStats() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent(MainActivity.ACTION_UPDATE_STATS)
                .putExtra("daily", dailyCoin)
                .putExtra("total", totalCoin)
        )
    }

    fun log(msg: String) {
        overlay.log(msg)
        // 同步到主界面日志
        broadcastStatus(msg)
    }
}