package com.xianyuzhushou.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions

class MlKitOcrEngine : OcrEngine {
    private val client = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    override fun recognize(bitmap: Bitmap): List<OcrText> {
        val image = InputImage.fromBitmap(bitmap, 0)
        return try {
            val result = Tasks.await(client.process(image))
            flatten(result)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun flatten(text: Text): List<OcrText> {
        val out = mutableListOf<OcrText>()
        for (block in text.textBlocks) {
            for (line in block.lines) {
                val r = line.boundingBox ?: block.boundingBox
                if (r != null) out.add(OcrText(line.text, Rect(r)))
            }
        }
        return out
    }
}