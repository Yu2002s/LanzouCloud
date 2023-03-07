package cc.drny.lanzou.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class LinearItemDecoration(private val size: Int): RecyclerView.ItemDecoration() {

    private var isFirst = true

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        if (isFirst) {
            isFirst = false
            outRect.top = size
        }
        outRect.bottom = size
        outRect.left = size
        outRect.right = size
    }
}