package cc.drny.lanzou.ui.file

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import cc.drny.lanzou.adapter.FileAdapter
import cc.drny.lanzou.data.download.Download
import cc.drny.lanzou.data.lanzou.LanzouFile
import cc.drny.lanzou.data.lanzou.LanzouPage
import cc.drny.lanzou.data.state.Completed
import cc.drny.lanzou.data.state.Empty
import cc.drny.lanzou.data.state.Error
import cc.drny.lanzou.data.state.LoadState
import cc.drny.lanzou.data.state.Loading
import cc.drny.lanzou.network.LanzouRepository
import cc.drny.lanzou.util.getIcon
import cc.drny.lanzou.util.getIconForExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.litepal.LitePal
import org.litepal.extension.find
import org.litepal.extension.findFirst

class FileViewModel(private val lanzouPage: LanzouPage) : ViewModel() {

    class FileViewModelFactory(private val lanzouPage: LanzouPage) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FileViewModel(lanzouPage) as T
        }
    }

    /**
     * 加载状态
     */
    val uiState = MutableLiveData<LoadState>()

    private var isNull = false

    private var isFirst = true
    private var isCompleted = false

    var isMultiMode = false

    var selectedCount = 0

    val lanzouFiles = mutableListOf<LanzouFile>()

    /**
     * 此方法只调用一次
     */
    @SuppressLint("NotifyDataSetChanged")
    fun getFiles(rv: RecyclerView) {
        if (!isFirst) {
            return
        }
        isFirst = false
        uiState.value = Loading

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                LanzouRepository.getFiles(lanzouPage).onSuccess {
                    if (it.isNotEmpty()) {
                        getIcon(rv.context, it)
                    }
                }
            }
            lanzouPage.page++
            val files = result.getOrNull()
            if (files == null) {
                uiState.value = Error(result.exceptionOrNull()?.message ?: "加载失败了")
                return@launch
            } else if (files.isEmpty()) {
                isNull = true
                uiState.value = Empty
                return@launch
            } else {
                val adapter = rv.adapter as FileAdapter
                lanzouFiles.clear()
                lanzouFiles.addAll(files)
                adapter.notifyData()
            }
            uiState.value = Completed
            isCompleted = true
        }
    }

    /**
     * 进行刷新
     */
    fun refresh(rv: RecyclerView) {
        isFirst = true
        lanzouPage.page = 1
        isNull = false
        isCompleted = false

        getFiles(rv)
    }

    /**
     * 加载更多数据
     */
    fun loadMore(rv: RecyclerView) {
        if (!isCompleted || isNull) {
            return
        }
        isCompleted = false
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                LanzouRepository.getFiles(lanzouPage).onSuccess {
                    if (it.isNotEmpty()) {
                        getIcon(rv.context, it)
                    }
                }
            }
            lanzouPage.page++
            result.onSuccess {
                if (it.isEmpty()) {
                    isNull = true
                } else {
                    lanzouFiles.addAll(it)
                    val itemCount = rv.adapter!!.itemCount
                    rv.adapter!!.notifyItemRangeInserted(
                        itemCount,
                        itemCount + it.size
                    )
                }
            }
            isCompleted = true
        }
    }

    private fun getIcon(context: Context, files: List<LanzouFile>) {
        files.forEach { lanzouFile ->
            if (lanzouFile.isFile()) {
                LanzouRepository.getFileRealExtension(lanzouFile)
                // 这里获取下载数据
                val download = LitePal.where("fileId = ?", lanzouFile.fileId.toString())
                    .findFirst<Download>()
                if (download != null && download.isCompleted()) {
                    lanzouFile.iconDrawable =
                        download.path.getIconForExtension(context, lanzouFile.icon)
                } else {
                    lanzouFile.iconDrawable = lanzouFile.icon.getIcon(context)
                }
            } else {
                lanzouFile.describe = lanzouFile.describe?.replace("[\\[\\]]".toRegex(), "")
                    //.replace("]", "")
            }
        }
    }

}