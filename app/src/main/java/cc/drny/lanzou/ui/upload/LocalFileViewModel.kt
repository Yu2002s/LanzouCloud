package cc.drny.lanzou.ui.upload

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import cc.drny.lanzou.LanzouApplication
import cc.drny.lanzou.R
import cc.drny.lanzou.data.upload.FileInfo
import cc.drny.lanzou.util.FileUtils.toSize
import cc.drny.lanzou.util.getIcon
import cc.drny.lanzou.util.sortFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class LocalFileViewModel(private val selected: List<FileInfo>) : ViewModel() {

    class LocalFileViewModelFactory(private val selected: List<FileInfo>) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LocalFileViewModel(selected) as T
        }
    }

    private var isFirst = true

    val localFileLiveData = MutableLiveData<List<FileInfo>>()

    fun refresh(context: Context, path: String) {
        isFirst = true
        viewModelScope.launch {
            getLocalFiles(context, path)
        }
    }

    suspend fun getLocalFiles(context: Context, path: String) {
        if (!isFirst) {
            return
        }
        isFirst = false
        // 获取本地文件
        val fileInfoList = mutableListOf<FileInfo>()
        withContext(Dispatchers.IO) {
            val file = File(path)
            val files = file.listFiles()
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
            val icon = ContextCompat.getDrawable(context, R.drawable.baseline_folder_24)
            files?.sortFile()?.forEach {
                val fileInfo = FileInfo(
                    name = it.name,
                    path = it.path,
                )
                val date = simpleDateFormat.format(Date(it.lastModified()))
                if (it.isFile) {
                    fileInfo.id = it.path.hashCode().toLong()
                    fileInfo.isSelected = selected.contains(fileInfo)
                    fileInfo.extension = it.extension
                    // fileInfo.icon  = fileInfo.extension.getIcon(LanzouApplication.context)
                    fileInfo.fileLength = it.length()
                    fileInfo.fileDesc = fileInfo.fileLength.toSize() + " " + date
                } else {
                    fileInfo.icon = icon
                    fileInfo.fileDesc = date
                }
                fileInfoList.add(fileInfo)
            }
        }
        Log.d("jdy", "loadData")
        localFileLiveData.value = fileInfoList
    }

}