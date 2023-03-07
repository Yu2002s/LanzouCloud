package cc.drny.lanzou.ui.analyze

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.drny.lanzou.data.lanzou.LanzouFile
import cc.drny.lanzou.data.lanzou.LanzouShareFile
import cc.drny.lanzou.network.LanzouRepository
import cc.drny.lanzou.util.getIcon
import cc.drny.lanzou.util.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnalyzeFolderViewModel : ViewModel() {

    private var page = 1

    private var isCompleted = true
    private var isNull = false

    fun isFirstPage() = page == 1

    val fileLiveData = MutableLiveData<MutableList<LanzouFile>>()

    fun getFolderList(context: Context, lanzouShareFile: LanzouShareFile) {
        if (!isCompleted || isNull) return
        isCompleted = false
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                LanzouRepository.getLanzouFilesForUrl(
                    lanzouShareFile.url,
                    lanzouShareFile.pwd,
                    page
                )
            }.onSuccess {
                if (it.size < 50) {
                    isNull = true
                    if (it.isEmpty()) {
                        return@onSuccess
                    }
                }
                it.forEach { lanzouFile ->
                    lanzouFile.iconDrawable = lanzouFile.icon.getIcon(context)
                }
                if (fileLiveData.value == null) {
                    fileLiveData.value = it as MutableList<LanzouFile>
                } else {
                    fileLiveData.value!!.addAll(it)
                }
                page ++
            }.onFailure {
                if (isFirstPage()) {
                    isNull = true
                    "获取资源失败".showToast()
                }
                fileLiveData.value = mutableListOf()
            }
            isCompleted = true
        }
    }

}