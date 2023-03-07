package cc.drny.lanzou.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.Menu
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.annotation.MenuRes
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.lifecycle.LifecycleCoroutineScope
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

abstract class BaseAdapter<T, V : ViewBinding>(private var mFilterData: List<T>) :
    RecyclerView.Adapter<BaseAdapter.ViewHolder<V>>(), Filterable {

    private val mSource = mFilterData

    companion object {
        const val NOTIFY_KEY = 0x01
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

    fun bindSearchView(menu: Menu, @MenuRes resId: Int) {
        val searchView = menu.findItem(resId).actionView as SearchView
        searchView.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                getFilter().filter(newText)
                return false
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
        })
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
            val last = lm.findLastVisibleItemPosition()
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

    /**
     * 删除具体条目
     */
    fun removeItem(position: Int) {
        (mFilterData as MutableList).removeAt(position)
        notifyItemRemoved(position)
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

    /**
     * 得到真实的数据（未搜索时的数据）
     */
    fun getDataBean(index: Int): DataBean<T> {
        var position = index
        var data = mFilterData[position]
        if (isFilterData()) {
            position = mSource.indexOf(data)
            data = mSource[position]
        }
        return DataBean(position, data)
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
                //val dataBean = getDataBean(viewHolder.adapterPosition)
                val position = viewHolder.adapterPosition
                val data = mFilterData[position]
                it.onItemClick(data, v)
                it.onItemClick(position, v)
                it.onItemClick(data, binding)
            }
        }
        onItemLongClickListener?.let {
            binding.root.setOnLongClickListener { v ->
                //val dataBean = getDataBean(viewHolder.adapterPosition)
                val position = viewHolder.adapterPosition
                val data = mFilterData[position]
                it.onItemLongClick(position, v)
                it.onItemLongClick(position, v)
                it.onItemLongClick(data, binding)
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
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                searchKeyWord = constraint.toString()
                val filterResult = FilterResults()
                if (searchKeyWord.isEmpty()) {
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
                mFilterData = results!!.values as MutableList<T>
                notifyDataSetChanged()

                // Log.d("jdy", "sources: ${mSource.size} , mFilter: ${mFilterData.size}")

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

    fun updateItem(position: Int, type: Int = getNotifyKeyNonNull(mFilterData[position])) {
        notifyItemChanged(position, type)
    }

}