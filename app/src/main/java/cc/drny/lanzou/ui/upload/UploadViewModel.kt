package cc.drny.lanzou.ui.upload

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.drny.lanzou.data.download.Download
import cc.drny.lanzou.data.upload.Upload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.litepal.LitePal
import org.litepal.extension.find
import org.litepal.extension.findAll

class UploadViewModel : ViewModel() {

    private lateinit var uploadLiveData: MutableLiveData<MutableList<Upload>>

    fun remove(position: Int) {
        uploadLiveData.value?.removeAt(position)
    }

    fun getUploadList(): LiveData<MutableList<Upload>> {
        if (!::uploadLiveData.isInitialized) {
            uploadLiveData = MutableLiveData()
            viewModelScope.launch {
                uploadLiveData.value = withContext(Dispatchers.IO) {
                    LitePal.order("time desc").limit(100).find(Upload::class.java)
                } ?: mutableListOf()
            }
        }
        return uploadLiveData
    }

    suspend fun addUpload(upload: Upload) {
        if (!::uploadLiveData.isInitialized) {
            uploadLiveData = MutableLiveData()
            val list = withContext(Dispatchers.IO) {
                LitePal.order("time desc").limit(100).find(Upload::class.java)
            }
            list.add(0, upload)
            if (uploadLiveData.value != null) {
                uploadLiveData.value!!.addAll(list)
            } else {
                uploadLiveData.value = list
            }
            Log.d("jdy", "init")
        } else {
            Log.d("jdy", "addUpload: $upload")
            if (uploadLiveData.value == null) {
                uploadLiveData.value = mutableListOf(upload)
            } else {
                uploadLiveData.value!!.add(0, upload)
            }
        }
    }

}