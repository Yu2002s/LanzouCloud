package cc.drny.lanzou.event

import androidx.viewbinding.ViewBinding

interface OnItemClickListener<T, V: ViewBinding> {

    fun onItemClick(position: Int, data: T , binding: V) {}

}