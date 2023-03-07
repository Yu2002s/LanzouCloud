package cc.drny.lanzou.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.ViewDebug.IntToString
import android.view.WindowManager
import cc.drny.lanzou.LanzouApplication

@SuppressLint("DiscouragedApi", "InternalInsetResource")
fun getNavigationBarHeight(): Int {
    val resources = LanzouApplication.context.resources
    val resId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return resources.getDimensionPixelSize(resId)
}

fun Int.dp2px(): Int {
    val res = LanzouApplication.context.resources
    return (res.displayMetrics.density * this + 0.5f).toInt()
}

fun getWindowHeight(): Int {
    val windowManager =
        LanzouApplication.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        windowManager.currentWindowMetrics.bounds.height()
    } else {
        windowManager.defaultDisplay.height
    }
}