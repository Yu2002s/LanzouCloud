package cc.drny.lanzou.ui.upload

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ClassificationViewModel(types: Array<Int>): ViewModel() {

    class ClassificationViewModelFactory(private val types: Array<Int>): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ClassificationViewModel(types) as T
        }
    }

    private val _liveData = MutableLiveData<List<UploadSelectorFragment>>()
    val liveData: LiveData<List<UploadSelectorFragment>> get() = _liveData

    init {
        _liveData.value = types.map { UploadSelectorFragment.newInstance(it) }
        Log.d("jdy", "fragments: ${liveData.value}")
    }

}