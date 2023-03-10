package cc.drny.lanzou.event
import androidx.viewbinding.ViewBinding

interface OnItemLongClickListener<T, V: ViewBinding> {

    fun onItemLongClick(data: T, position: Int, binding: V)

}