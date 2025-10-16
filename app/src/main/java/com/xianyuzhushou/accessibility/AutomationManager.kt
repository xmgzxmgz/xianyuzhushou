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

    // 完成/已领取标识关键词（用于提示任务完成）
    private val completedKeywords = listOf(
        "已完成", "完成", "已领取", "已收取", "今日已领"
    )

    fun performOneCycle(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        var acted = false

        // 1) 优先尝试领取奖励
        if (clickByText(root, rewardKeywords)) {
            val gain = detectRewardAmount(root) ?: estimateCoinGain()
            service.onRewardClaimed(gain)
            acted = true
            sleepShort()
        }

        // 2) 识别任务列表的“去完成”并按任务名称路由
        val routed = routeTasksByName(root)
        if (routed) {
            acted = true
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

    fun tryDailySignIn(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        // 先检测“明日再来”提示
        val tomorrow = root.findAccessibilityNodeInfosByText("明日再来")
        if (!tomorrow.isNullOrEmpty()) {
            service.log("今日签到已完成（明日再来）")
            return true
        }
        // 查找“签到”按钮并点击
        val signNodes = root.findAccessibilityNodeInfosByText("签到")
        if (!signNodes.isNullOrEmpty()) {
            for (n in signNodes) {
                if (performClickSafely(n)) {
                    service.log("正在签到…")
                    sleepMedium()
                    return true
                }
            }
        }
        return false
    }

    private fun routeTasksByName(root: AccessibilityNodeInfo): Boolean {
        val goNodes = root.findAccessibilityNodeInfosByText("去完成")
        if (goNodes.isNullOrEmpty()) return false
        for (btn in goNodes) {
            val taskName = nearbyText(btn) ?: continue
            // 跳过不要完成的任务
            if (taskName.contains("购买宝贝") || taskName.contains("发布一件新宝贝") || taskName.contains("收藏三个")) {
                service.log("跳过任务：$taskName")
                continue
            }
            service.log("即将执行任务：$taskName")
            if (performClickSafely(btn)) {
                sleepMedium()
                // 路由到具体处理
                when {
                    taskName.contains("浏览指定频道好物") -> {
                        handleBrowseForSeconds(root, 20_000)
                        returnBack(1)
                        service.onRewardClaimed(detectRewardAmount(root) ?: estimateCoinGain())
                        service.log("任务已完成：$taskName")
                        return true
                    }
                    taskName.contains("蚂蚁庄园") || taskName.contains("蚂蚁森林") || taskName.contains("八八农场") || taskName.contains("水果") -> {
                        // 跳转到支付宝后再返回
                        handleAlipayJumpAndReturn(root)
                        service.onRewardClaimed(detectRewardAmount(root) ?: estimateCoinGain())
                        service.log("任务已完成：$taskName")
                        return true
                    }
                    taskName.contains("搜一搜推荐商品") -> {
                        // 进入后随意点一个商品
                        randomClickAnyItem(root)
                        handleBrowseForSeconds(root, 20_000)
                        returnBack(2)
                        service.onRewardClaimed(detectRewardAmount(root) ?: estimateCoinGain())
                        service.log("任务已完成：$taskName")
                        return true
                    }
                    taskName.contains("拼手气红包") -> {
                        // 进入后点击“做任务参与”，随意点商品并浏览，返回三次
                        clickByText(root, listOf("做任务参与"))
                        sleepMedium()
                        randomClickAnyItem(root)
                        handleBrowseForSeconds(root, 20_000)
                        returnBack(3)
                        service.onRewardClaimed(500) // 明确奖励 500 币
                        service.log("任务已完成：$taskName，奖励500币")
                        return true
                    }
                    else -> {
                        // 未覆盖的任务，退回到通用流程：点击后尝试滚动/等待再返回
                        handleBrowseForSeconds(root, 10_000)
                        returnBack(1)
                        service.onRewardClaimed(detectRewardAmount(root) ?: estimateCoinGain())
                        service.log("任务完成（通用）：$taskName")
                        return true
                    }
                }
            }
        }
        return false
    }

    fun checkTaskCompleted(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        for (kw in completedKeywords) {
            val nodes = root.findAccessibilityNodeInfosByText(kw)
            if (!nodes.isNullOrEmpty()) return true
        }
        return false
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

    private fun returnBack(times: Int) {
        repeat(times) {
            service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            sleepShort()
        }
    }

    private fun handleBrowseForSeconds(root: AccessibilityNodeInfo, millis: Long) {
        val steps = (millis / 3000).toInt()
        for (i in 0 until steps) {
            // 尝试轻微滚动
            scrollForward(root)
            SystemClock.sleep(3000)
        }
    }

    private fun handleAlipayJumpAndReturn(root: AccessibilityNodeInfo) {
        // 等待切到支付宝
        var waited = 0
        while (waited < 8000) {
            val pkg = root.packageName?.toString() ?: ""
            if (pkg.contains("Alipay", ignoreCase = true) || pkg == "com.eg.android.AlipayGphone") break
            SystemClock.sleep(500)
            waited += 500
        }
        // 等待2秒，然后返回直到回到闲鱼
        SystemClock.sleep(2000)
        var backCount = 0
        while (backCount < 5) {
            val pkg = root.packageName?.toString() ?: ""
            if (pkg == "com.taobao.idlefish") break
            service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            SystemClock.sleep(800)
            backCount++
        }
    }

    private fun randomClickAnyItem(root: AccessibilityNodeInfo): Boolean {
        // 简单策略：寻找任何可点击的商品文本或容器
        val candidates = root.findAccessibilityNodeInfosByText("") // 空文本不行，遍历树
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(root)
        var clicked = false
        while (stack.isNotEmpty()) {
            val n = stack.removeFirst()
            if (!clicked && n.isClickable) {
                if (n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    clicked = true
                }
            }
            for (i in 0 until n.childCount) {
                n.getChild(i)?.let { stack.add(it) }
            }
            if (clicked) break
        }
        return clicked
    }

    private fun nearbyText(node: AccessibilityNodeInfo): String? {
        // 取父节点与兄弟节点的文本作为任务名称候选
        val parent = node.parent ?: return null
        val texts = mutableListOf<String>()
        parent.text?.toString()?.let { if (it.isNotBlank()) texts.add(it) }
        for (i in 0 until parent.childCount) {
            parent.getChild(i)?.let { child ->
                child.text?.toString()?.let { if (it.isNotBlank()) texts.add(it) }
                child.contentDescription?.toString()?.let { if (it.isNotBlank()) texts.add(it) }
            }
        }
        // 返回最长的文本作为任务名称（通常是标题）
        return texts.maxByOrNull { it.length }
    }

    private fun sleepShort() { SystemClock.sleep(600) }
    private fun sleepMedium() { SystemClock.sleep(1200) }

    /**
     * 由于无法准确解析奖励金额，这里进行一个温和估算：1~5随机，用于界面统计参考。
     * 真正的币数以用户页面为准。
     */
    private fun estimateCoinGain(): Int = Random.nextInt(1, 6)

    /**
     * 尝试从当前窗口的文本中解析奖励金额：例如“+80”“获得80币”“闲鱼币 100”。
     * 若未能解析则返回null。
     */
    private fun detectRewardAmount(root: AccessibilityNodeInfo): Int? {
        val texts = mutableListOf<String>()
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(root)
        while (stack.isNotEmpty()) {
            val n = stack.removeFirst()
            n.text?.toString()?.let { if (it.isNotBlank()) texts.add(it) }
            n.contentDescription?.toString()?.let { if (it.isNotBlank()) texts.add(it) }
            for (i in 0 until n.childCount) {
                n.getChild(i)?.let { stack.add(it) }
            }
        }
        var best: Int? = null
        for (t in texts) {
            // 形如 +80 或 + 80
            Regex("\\+\\s?(\\d+)").find(t)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
                best = maxOf(best ?: 0, it)
            }
            // 形如 80币 / 获得80币 / 闲鱼币 80
            Regex("(\\d+)\\s*币").find(t)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
                best = maxOf(best ?: 0, it)
            }
            Regex("闲鱼币\\s*(\\d+)").find(t)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
                best = maxOf(best ?: 0, it)
            }
        }
        return best?.takeIf { it in 1..2000 }
    }
}