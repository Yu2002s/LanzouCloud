package cc.drny.lanzou.ui.download

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.drny.lanzou.data.download.Download
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.litepal.LitePal
import org.litepal.extension.findAll

class DownloadViewModel: ViewModel() {

    private lateinit var downloadLiveData: MutableLiveData<MutableList<Download>>

    fun getDownloadList(): LiveData<MutableList<Download>> {
        if (!::downloadLiveData.isInitialized) {
            Log.d("jdy", "init")
            downloadLiveData = MutableLiveData()
            viewModelScope.launch {
                downloadLiveData.value = withContext(Dispatchers.IO) {
                    LitePal.order("time desc").limit(100).find(Download::class.java)
                } ?: mutableListOf()
            }
        }
        return downloadLiveData
    }

    fun removeDownload(index: Int) {
        downloadLiveData.value?.removeAt(index)
    }

    suspend fun addDownload(download: Download) {
        if (!::downloadLiveData.isInitialized) {
            downloadLiveData = MutableLiveData()
            val list = withContext(Dispatchers.IO) {
                LitePal.order("time desc").limit(100).find(Download::class.java)
            }
            list.add(0, download)
            if (downloadLiveData.value != null) {
                Log.d("jdy", "333")
                downloadLiveData.value!!.addAll(list)
            } else {
                Log.d("jdy", "444")
                downloadLiveData.value = list
            }
        } else {
            if (downloadLiveData.value == null) {
                Log.d("jdy", "111")
                downloadLiveData.value = mutableListOf(download)
            } else {
                Log.d("jdy", "222")
                downloadLiveData.value!!.add(0, download)
            }

        }
    }

}
