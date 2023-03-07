package cc.drny.lanzou.event

import android.view.View
import androidx.viewbinding.ViewBinding

interface OnItemClickListener<T, V: ViewBinding> {

    fun onItemClick(position: Int, v: View) {}

    fun onItemClick(data: T, v: View) {}

    fun onItemClick(data: T, binding: V) {}

}