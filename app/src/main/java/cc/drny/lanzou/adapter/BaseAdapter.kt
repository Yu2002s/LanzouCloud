package cc.drny.lanzou.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.annotation.RestrictTo
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.SavedStateHandle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.viewbinding.ViewBinding
import cc.drny.lanzou.data.DataBean
import cc.drny.lanzou.event.OnItemClickListener
import cc.drny.lanzou.event.OnItemLongClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BaseAdapter<T : Any, V : ViewBinding>(private var mFilterData: MutableList<T>) :
    RecyclerView.Adapter<BaseAdapter.ViewHolder<V>>(), Filterable {

    private val mSource = mFilterData

    companion object {
        const val NOTIFY_KEY = 0x01
        const val SEARCH_KEY = "search_key"
    }

    var onItemClickListener: OnItemClickListener<T, V>? = null

    var onItemLongClickListener: OnItemLongClickListener<T, V>? = null

    var searchKeyWord = ""

    /**
     *  默认关闭自动加载功能
     */
    private var enableAutoLoad: Boolean = false

    private lateinit var coroutineScope: LifecycleCoroutineScope

    private lateinit var recyclerView: RecyclerView

    fun getList() = mFilterData

    fun isSearch() = searchKeyWord.isNotBlank()

    fun enableAutoLoad(): BaseAdapter<T, V> {
        enableAutoLoad = true
        return this
    }

    fun scopeIn(coroutineScope: LifecycleCoroutineScope): BaseAdapter<T, V> {
        this.coroutineScope = coroutineScope
        return this
    }

    @SuppressLint("NotifyDataSetChanged")
    fun notifyData() {
        Log.d("jdy", "searchKey: $searchKeyWord")
        if (searchKeyWord.isNotEmpty()) {
            filter(searchKeyWord)
        } else {
            notifyDataSetChanged()
        }
        if (::recyclerView.isInitialized) {
            recyclerView.scheduleLayoutAnimation()
        }
    }

    fun isFilterData() = mFilterData.size != mSource.size

    fun bindSearchView(menu: Menu, @IdRes resId: Int) {
        val searchView = menu.findItem(resId).actionView as SearchView
        searchView.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                getFilter().filter(newText)
                return true
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
        })
    }

    /**
     * 删除具体条目
     */
    fun removeSimpleItem(position: Int) {
        mFilterData.removeAt(position)
        notifyItemRemoved(position)
    }

    fun removeItems(position: Int, data: T = mSource[position]): Int {
        var index = position
        mSource.removeAt(position)
        if (isFilterData()) {
            index = mFilterData.indexOf(data)
            if (index == -1) return -1
            mFilterData.removeAt(index)
        }
        notifyItemRemoved(index)
        return index
    }

    fun addItem(position: Int, data: T) {
        mSource.add(position, data)
        if (isFilterData()) {
            mFilterData.add(position, data)
        }
        notifyItemInserted(position)
    }

    fun addItem(data: T) {
        addItem(0, data)
    }

    fun updateItem(position: Int, type: Int, data: T = mFilterData[position]) {
        updateItem(position, data, type)
    }

    fun updateItem(position: Int, data: T = mFilterData[position], type: Int = getNotifyKeyNonNull(data)) {
        notifyItemChanged(position, type)
    }

    fun updateItems(position: Int, data: T = mSource[position], type: Int = getNotifyKeyNonNull(data)) {
        var index = position
        if (isFilterData()) {
            index = mFilterData.indexOf(data)
        }
        if (index == -1) return
        notifyItemChanged(index, type)
    }

    class ViewHolder<V : ViewBinding>(val viewBinding: V) :
        RecyclerView.ViewHolder(viewBinding.root)

    private val scrollListener by lazy {
        object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    getData()
                }
            }
        }
    }

    private fun getData() {
        // 此处进行加载
        val lm = recyclerView.layoutManager
        if (lm is LinearLayoutManager) {
            val first = lm.findFirstVisibleItemPosition()
            var last = lm.findLastVisibleItemPosition()
            if (last > mFilterData.size - 1) {
                last = mFilterData.size - 1
            }
            if (first == -1 || last == -1) return
            for (i in first..last) {
                val bean = mFilterData[i]
                if (!isLoad(bean)) continue
                coroutineScope.launch {
                    val any = withContext(Dispatchers.IO) {
                        getData(recyclerView.context, bean)
                    }
                    notifyItemChanged(i, any ?: getNotifyKeyNonNull(bean))
                }
            }
        }
    }

    fun update() {
        if (!::recyclerView.isInitialized) return
        if (mFilterData.isEmpty()) return
        recyclerView.postDelayed({
            getData()
        }, 500)
    }

    fun restoreSearchKey(item: MenuItem) {
        Log.d("jdy", "search: " +  searchKeyWord)
        if (isSearch()) {
            item.expandActionView()
            val actionView = item.actionView
            if (actionView is SearchView) {
                actionView.setQuery(searchKeyWord, false)
            }
        }
    }

    fun getSavedSearchKey(savedStateBundle: Bundle?) {
        savedStateBundle?.let {
            searchKeyWord = it.getString(SEARCH_KEY, "")
        }
    }

    fun putSearchKey(savedStateBundle: Bundle?) {
        savedStateBundle?.putString(SEARCH_KEY, searchKeyWord)
    }

    /**
     * 获取具体的数据位置信息
     */
    fun getPosition(index: Int, data: T): Int {
        return if (isFilterData()) {
            mFilterData.indexOf(data)
        } else {
            index
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        if (!enableAutoLoad) return
        recyclerView.addOnScrollListener(scrollListener)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)

        if (!enableAutoLoad) return

        recyclerView.removeOnScrollListener(scrollListener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<V> {
        val binding = onCreateBinding(parent, viewType)
        val viewHolder = ViewHolder(binding)
        onItemClickListener?.let {
            binding.root.setOnClickListener { v ->
                val position = viewHolder.adapterPosition
                val data = mFilterData[position]
                it.onItemClick(position, data, binding)
            }
        }
        onItemLongClickListener?.let {
            binding.root.setOnLongClickListener { v ->
                val position = viewHolder.adapterPosition
                val data = mFilterData[position]
                it.onItemLongClick(data, position, binding)
                true
            }
        }
        onViewHolderCreated(viewHolder)
        return viewHolder
    }

    override fun getItemCount() = mFilterData.size

    override fun onBindViewHolder(holder: ViewHolder<V>, position: Int) {
        onBindView(mFilterData[position], holder.viewBinding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder<V>,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            onBindView(mFilterData[position], holder.viewBinding)
            return
        }
        onBindView(mFilterData[position], holder.viewBinding, payloads[0])
    }

    private val filter by lazy {
        object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults? {
                searchKeyWord = constraint.toString()
                val filterResult = FilterResults()
                if (searchKeyWord.isEmpty()) {
                    if (mSource == mFilterData) {
                        return null
                    }
                    filterResult.values = mSource
                    filterResult.count = mSource.size
                } else {
                    val filteredData = mutableListOf<T>()
                    mSource.forEach {
                        if (onFilter(searchKeyWord, it)) {
                            filteredData.add(it)
                        }
                    }
                    filterResult.values = filteredData
                    filterResult.count = filteredData.size
                }
                return filterResult
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results == null) return
                mFilterData = results.values as MutableList<T>
                notifyDataSetChanged()

                if (::recyclerView.isInitialized) {
                    val runnable = Runnable {
                        getData()
                    }
                    recyclerView.post(runnable)
                }
            }
        }
    }

    /*@SuppressLint("NotifyDataSetChanged")
    fun notifyData() {
        if (searchKeyWord.isNotEmpty()) {
            filter()
        } else {
            notifyDataSetChanged()
        }
    }*/

    fun filter(key: String? = searchKeyWord) {
        filter.filter(key)
    }

    override fun getFilter() = filter

    abstract fun onCreateBinding(parent: ViewGroup, viewType: Int): V

    abstract fun onBindView(data: T, binding: V)

    open fun onBindView(data: T, binding: V, type: Any) {}

    override fun getItemViewType(position: Int): Int {
        return getViewType(mFilterData[position])
    }

    open fun getViewType(data: T): Int {
        return 0
    }

    open fun getNotifyKey(data: T): Int? {
        return null
    }

    private fun getNotifyKeyNonNull(data: T): Int {
        return getNotifyKey(data) ?: NOTIFY_KEY
    }

    open fun getData(context: Context, data: T): Any? {
        return NOTIFY_KEY
    }

    open fun isLoad(data: T): Boolean {
        return true
    }

    open fun onFilter(key: String, data: T): Boolean {
        return true
    }

    open fun onViewHolderCreated(holder: ViewHolder<V>) {}

}