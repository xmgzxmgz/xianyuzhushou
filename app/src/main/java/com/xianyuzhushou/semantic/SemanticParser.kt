package com.xianyuzhushou.semantic

import android.graphics.Rect
import com.xianyuzhushou.ocr.OcrText

data class ActionCandidate(val type: Type, val rect: Rect, val title: String?) {
    enum class Type { GO, CLAIM }
}

class SemanticParser {
    private val goKeywords = listOf("去完成", "去完成任务")
    private val claimKeywords = listOf("领取奖励", "去领取", "收下奖励", "领取")

    fun parse(texts: List<OcrText>): List<ActionCandidate> {
        val out = mutableListOf<ActionCandidate>()
        for (t in texts) {
            val txt = t.text
            when {
                goKeywords.any { txt.contains(it) } -> {
                    out.add(ActionCandidate(ActionCandidate.Type.GO, Rect(t.rect), nearestTitle(texts, t)))
                }
                claimKeywords.any { txt.contains(it) } -> {
                    out.add(ActionCandidate(ActionCandidate.Type.CLAIM, Rect(t.rect), nearestTitle(texts, t)))
                }
            }
        }
        return out.sortedBy { it.rect.top }
    }

    private fun nearestTitle(all: List<OcrText>, ref: OcrText): String? {
        // 在同一水平带内（y接近）寻找位于左侧且字符较长的文本作为标题
        var best: OcrText? = null
        var bestScore = Double.MAX_VALUE
        for (t in all) {
            if (t === ref) continue
            val dy = Math.abs((t.rect.centerY()) - (ref.rect.centerY()))
            val dx = ref.rect.left - t.rect.right
            if (dx <= 0) continue // 只找左侧文本
            val len = t.text.length
            if (len < 4) continue
            val score = dy + dx * 0.2
            if (score < bestScore) {
                bestScore = score
                best = t
            }
        }
        return best?.text
    }
}