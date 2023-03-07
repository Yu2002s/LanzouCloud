package cc.drny.lanzou.ui.upload

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.drny.lanzou.R
import cc.drny.lanzou.data.upload.FileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppDataViewModel: ViewModel() {

    private lateinit var appLiveData: MutableLiveData<List<FileInfo>>

    fun getApps(context: Context): LiveData<List<FileInfo>> {
        if (!::appLiveData.isInitialized) {
            appLiveData = MutableLiveData()
            viewModelScope.launch {
                val androidPath = "/storage/emulated/0/Android/data/"
                val icon = ContextCompat.getDrawable(context, R.drawable.baseline_folder_24)
                val fileList = withContext(Dispatchers.IO) {
                   try {
                       val pm =  context.packageManager
                       val packageInfos = pm.getInstalledPackages(0)
                       packageInfos
                           .filter { it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                           .map {
                           val applicationInfo = it.applicationInfo
                           val path = androidPath + it.packageName
                           FileInfo(-1, applicationInfo.loadLabel(pm).toString(), path, -1, it.packageName).apply {
                               this.icon = icon
                           }
                       }
                   } catch (e: Exception) {
                       mutableListOf()
                   }
                }
                appLiveData.value = fileList
            }
        }
        return appLiveData
    }

}