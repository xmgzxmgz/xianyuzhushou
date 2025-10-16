package com.xianyuzhushou.ocr

import android.graphics.Bitmap
import android.graphics.Rect

data class OcrText(val text: String, val rect: Rect)

interface OcrEngine {
    fun recognize(bitmap: Bitmap): List<OcrText>
}