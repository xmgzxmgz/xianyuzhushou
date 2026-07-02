package com.xianyuzhushou.accessibility

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OverlayController(private val context: Context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: View? = null
    private lateinit var tvLog: TextView
    private lateinit var scroll: ScrollView
    private var closedByUser = false
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun show() {
        if (closedByUser) return
        if (view != null) return
        val inflater = LayoutInflater.from(context)
        val v = inflater.inflate(com.xianyuzhushou.R.layout.overlay_log, null)
        tvLog = v.findViewById(com.xianyuzhushou.R.id.tvOverlayLog)
        scroll = v.findViewById(com.xianyuzhushou.R.id.overlayScroll)
        v.findViewById<Button>(com.xianyuzhushou.R.id.btnOverlayClose).setOnClickListener {
            hide()
            closedByUser = true
        }

        val dm = context.resources.displayMetrics
        val width = dm.widthPixels // 顶部全宽
        val height = (dm.heightPixels * 0.25).toInt() // 高占屏幕四分之一

        val lp = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.y = 20
        lp.x = 0

        wm.addView(v, lp)
        view = v
    }

    fun hide() {
        view?.let { wm.removeView(it) }
        view = null
    }

    fun resetClosedFlag() { closedByUser = false }

    fun log(msg: String) {
        if (view == null) return
        val line = "${timeFmt.format(Date())}  $msg"
        tvLog.append("\n$line")
        tvLog.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }
}