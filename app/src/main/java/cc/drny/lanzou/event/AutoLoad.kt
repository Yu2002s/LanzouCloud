package cc.drny.lanzou.event

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import kotlinx.coroutines.CoroutineScope

abstract class AutoLoad<T> {

    abstract fun bindView(rv: RecyclerView, coroutineScope: CoroutineScope)

    abstract suspend fun getIcon(bean: T)

    abstract fun isLoad(bean: T): Boolean
}