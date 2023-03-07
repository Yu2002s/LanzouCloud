package cc.drny.lanzou.ui.upload

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cc.drny.lanzou.R
import cc.drny.lanzou.data.upload.FileInfo
import cc.drny.lanzou.util.DateUtils.handleTime
import cc.drny.lanzou.util.FileUtils.toSize
import cc.drny.lanzou.util.sortFile
import cc.drny.lanzou.util.uri2Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AndroidDataViewModel(private val selected: List<FileInfo>) : ViewModel() {

    class AndroidDataViewModelFactory(private val selected: List<FileInfo>): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AndroidDataViewModel(selected) as T
        }
    }

    val fileLiveData = MutableLiveData<List<FileInfo>>()

    private var isFirst = true

    fun refreshFiles(context: Context, documentFile: DocumentFile) {
        isFirst = true
        viewModelScope.launch {
            getFiles(context, documentFile)
        }
    }

    suspend fun getFiles(context: Context, documentFile: DocumentFile) {
        if (!isFirst) {
            return
        }
        isFirst = false
        val list = withContext(Dispatchers.IO) {
            val folderIcon = ContextCompat.getDrawable(context, R.drawable.baseline_folder_24)
            // val documentFile = DocumentFile.fromTreeUri(context, uri)

            documentFile.listFiles()
                // ?.filter { it.isFile }
                .map {
                    val contentUri = it.uri
                    val id = DocumentsContract.getDocumentId(contentUri)
                    val modified = it.lastModified()

                    val fileInfo = FileInfo(
                        id.hashCode().toLong(),
                        it.name!!,
                        contentUri.toString()
                    )
                    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(it.type)
                    if (it.isFile) {
                        val length = it.length()
                        fileInfo.fileLength = length
                        fileInfo.fileDesc = length.toSize() + " - " + modified.handleTime()
                        fileInfo.extension = extension
                    } else {
                        fileInfo.path = contentUri.uri2Path()
                        fileInfo.fileDesc = modified.handleTime()
                        fileInfo.icon = folderIcon
                    }

                    fileInfo.isSelected = selected.contains(fileInfo)
                    fileInfo
                }.sortFile()
        }
        fileLiveData.value = list
    }

}