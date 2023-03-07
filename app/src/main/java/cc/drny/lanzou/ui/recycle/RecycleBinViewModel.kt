package cc.drny.lanzou.ui.recycle

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cc.drny.lanzou.data.lanzou.LanzouFile
import cc.drny.lanzou.network.LanzouRepository

class RecycleBinViewModel: ViewModel() {

    var isMultiMode = false
    var selectedCount = 0

    private var fileLiveData: LiveData<Result<List<LanzouFile>>>? = null

    fun recycleFiles(context: Context): LiveData<Result<List<LanzouFile>>> {
        if (fileLiveData == null) {
            fileLiveData = LanzouRepository.getRecycleBinFile(context)
        }
        return fileLiveData!!
    }

}