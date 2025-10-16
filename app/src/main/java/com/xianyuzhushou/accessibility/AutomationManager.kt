package com.xianyuzhushou.accessibility

import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.random.Random

/**
 * 负责在当前窗口中执行一次任务循环：点击按钮、滚动列表、尝试领取奖励。
 * 使用尽量稳健的查找与点击策略，避免误触与死循环。
 */
class AutomationManager(private val service: XianyuAccessibilityService) {

    private val TAG = "XyAuto"

    // 任务按钮关键词：可根据后续页面文案调整
    private val actionKeywords = listOf(
        "去完成", "领红包", "领取奖励", "签到", "领取", "去完成任务"
    )

    // 奖励提示/按钮关键词
    private val rewardKeywords = listOf(
        "领取奖励", "去领取", "收下奖励", "领取"
    )

    fun performOneCycle(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        var acted = false

        // 1) 优先尝试领取奖励
        if (clickByText(root, rewardKeywords)) {
            service.onRewardClaimed(estimateCoinGain())
            acted = true
            sleepShort()
        }

        // 2) 点击任务按钮
        if (clickByText(root, actionKeywords)) {
            acted = true
            sleepMedium()
        }

        // 3) 尝试滚动任务列表
        if (!acted) {
            if (scrollForward(root)) {
                acted = true
                sleepShort()
            }
        }

        return acted
    }

    private fun clickByText(root: AccessibilityNodeInfo, keywords: List<String>): Boolean {
        for (kw in keywords) {
            val nodes = root.findAccessibilityNodeInfosByText(kw)
            if (nodes.isNullOrEmpty()) continue
            for (n in nodes) {
                if (performClickSafely(n)) {
                    Log.d(TAG, "点击：$kw")
                    return true
                }
            }
        }
        return false
    }

    private fun performClickSafely(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        // 如果自身不可点击，尝试寻找可点击的父节点
        var cur: AccessibilityNodeInfo? = node
        var steps = 0
        while (cur != null && steps < 5) {
            if (cur.isClickable) {
                return cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            cur = cur.parent
            steps++
        }
        // 尝试使用可选择的动作，如focus + click
        return node.performAction(AccessibilityNodeInfo.ACTION_FOCUS) &&
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun scrollForward(root: AccessibilityNodeInfo): Boolean {
        // 优先滚动标记为scrollable的节点
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(root)
        while (stack.isNotEmpty()) {
            val n = stack.removeFirst()
            if (n.isScrollable) {
                if (n.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) return true
            }
            for (i in 0 until n.childCount) {
                n.getChild(i)?.let { stack.add(it) }
            }
        }
        // 找不到则尝试对根窗口做通用滚动手势（部分机型可能无效）
        return root.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    private fun sleepShort() { SystemClock.sleep(600) }
    private fun sleepMedium() { SystemClock.sleep(1200) }

    /**
     * 由于无法准确解析奖励金额，这里进行一个温和估算：1~5随机，用于界面统计参考。
     * 真正的币数以用户页面为准。
     */
    private fun estimateCoinGain(): Int = Random.nextInt(1, 6)
}