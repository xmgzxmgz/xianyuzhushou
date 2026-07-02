package com.xianyuzhushou.ocr

import android.content.Context
import android.graphics.Bitmap

/**
 * tess-two 备用引擎占位实现：由于需要训练数据，默认不启用。
 * 若配置了数据文件，可扩展为实际识别。
 */
class TesseractOcrEngine(private val context: Context) : OcrEngine {
    override fun recognize(bitmap: Bitmap): List<OcrText> {
        // TODO: 配置 tessdata 后接入实际识别
        return emptyList()
    }
}