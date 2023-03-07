package cc.drny.lanzou.event

import android.view.View
import androidx.viewbinding.ViewBinding

interface OnItemLongClickListener<T, V: ViewBinding> {

    fun onItemLongClick(position: Int, v: View) {}

    fun onItemLongClick(data: T, v: View) {}

    fun onItemLongClick(data: T, binding: V) {}

}