package com.xianyuzhushou.capture

import android.graphics.Bitmap
import android.os.Build
import android.view.Display
import com.xianyuzhushou.accessibility.XianyuAccessibilityService

/**
 * 截图管理：Android 13+ 使用 AccessibilityService.takeScreenshot；
 * 当前为兼容编译，若不可用则返回 null。后续将接入 MediaProjection 完整方案。
 */
class ScreenshotManager(private val service: XianyuAccessibilityService) {

    fun captureFull(): Bitmap? {
        // 为保持跨版本编译稳定，当前不启用截图；后续接入 MediaProjection
        return null
    }

    fun captureTiles(): Pair<Bitmap?, Bitmap?> {
        val full = captureFull() ?: return Pair(null, null)
        val dm = service.resources.displayMetrics
        val w = dm.widthPixels
        val h = dm.heightPixels
        val leftW = (w * 0.66).toInt()
        val rightW = w - leftW
        val tileH = h
        val left = Bitmap.createBitmap(full, 0, 0, leftW, tileH)
        val right = Bitmap.createBitmap(full, leftW, 0, rightW, tileH)
        return Pair(left, right)
    }
}