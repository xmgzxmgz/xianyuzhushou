package com.xianyuzhushou.validator

import com.xianyuzhushou.accessibility.XianyuAccessibilityService
import com.xianyuzhushou.capture.ScreenshotManager
import com.xianyuzhushou.image.ImagePreprocessor
import com.xianyuzhushou.ocr.MlKitOcrEngine

/**
 * 执行后闭环校验：截图+OCR，匹配“已完成/已领取/奖励到账/明日再来”。
 */
class Validator(private val service: XianyuAccessibilityService) {
    private val capture = ScreenshotManager(service)
    private val pre = ImagePreprocessor()
    private val ocr = MlKitOcrEngine()
    private val keywords = listOf("已完成", "已领取", "奖励到账", "明日再来")

    fun checkCompletedOrClaimed(): Boolean {
        val (left, right) = capture.captureTiles()
        val bmp = left ?: right ?: return false
        val prep = pre.preprocessForText(bmp)
        val texts = ocr.recognize(prep)
        return texts.any { t -> keywords.any { k -> t.text.contains(k) } }
    }
}