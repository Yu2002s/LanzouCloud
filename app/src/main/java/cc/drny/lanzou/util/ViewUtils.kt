package cc.drny.lanzou.util

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.StaggeredGridLayoutManager

fun RecyclerView.findLastVisiblePosition(): Int {
    if (layoutManager is StaggeredGridLayoutManager) {
        val itemCount = adapter?.itemCount ?: 0
        val positions =
            (layoutManager as StaggeredGridLayoutManager).findLastVisibleItemPositions(null)
        val index = positions.max()
        return if (index + 1 < itemCount - 1) index + 1 else index
    } else if (layoutManager is GridLayoutManager) {
        return (layoutManager as GridLayoutManager).findLastVisibleItemPosition()
    }
    return -1
}

fun RecyclerView.findFirstVisiblePosition(): Int {
    if (layoutManager is StaggeredGridLayoutManager) {
        val positions =
            (layoutManager as StaggeredGridLayoutManager).findFirstVisibleItemPositions(null)
        return positions.min()
    }
    return -1
}