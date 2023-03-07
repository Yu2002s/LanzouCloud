package cc.drny.lanzou.util

import android.annotation.SuppressLint
import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Menu
import android.view.MenuInflater
import android.widget.Toast
import androidx.appcompat.view.menu.MenuBuilder
import cc.drny.lanzou.LanzouApplication

fun String?.showToast() {
    if (this == null) return
    Toast.makeText(LanzouApplication.context, this, Toast.LENGTH_SHORT).show()
}

@SuppressLint("RestrictedApi")
fun Menu.enableMenuIcon() {
    if (this is MenuBuilder) {
        this.setOptionalIconsVisible(true)
        for (item in this.visibleItems) {
            val resources = LanzouApplication.context.resources
            val iconMarginPx =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics)
                    .toInt()
            if (item.icon != null) {
                item.icon = InsetDrawable(item.icon, iconMarginPx, 0, iconMarginPx, 0)
            }
        }
    }
}