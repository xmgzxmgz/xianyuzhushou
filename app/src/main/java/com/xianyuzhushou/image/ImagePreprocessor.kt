package com.xianyuzhushou.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * 纯Android图像预处理：灰度+简单自适应阈值。
 * 后续可替换为 OpenCV 实现以获得更好效果。
 */
class ImagePreprocessor {

    fun preprocessForText(src: Bitmap): Bitmap {
        // 先灰度
        val gray = toGrayscale(src)
        // 简易自适应阈值：计算局部块平均并二值化（粗略）
        return adaptiveThreshold(gray)
    }

    private fun toGrayscale(src: Bitmap): Bitmap {
        val bmp = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return bmp
    }

    private fun adaptiveThreshold(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val block = 16
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        val outPx = IntArray(w * h)
        for (y in 0 until h step block) {
            for (x in 0 until w step block) {
                var sum = 0
                var cnt = 0
                val yEnd = minOf(y + block, h)
                val xEnd = minOf(x + block, w)
                for (yy in y until yEnd) {
                    for (xx in x until xEnd) {
                        val c = pixels[yy * w + xx]
                        val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
                        val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                        sum += lum; cnt++
                    }
                }
                val mean = sum / maxOf(cnt, 1)
                val thr = mean - 10
                for (yy in y until yEnd) {
                    for (xx in x until xEnd) {
                        val c = pixels[yy * w + xx]
                        val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
                        val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                        outPx[yy * w + xx] = if (lum > thr) Color.WHITE else Color.BLACK
                    }
                }
            }
        }
        out.setPixels(outPx, 0, w, 0, 0, w, h)
        return out
    }
}