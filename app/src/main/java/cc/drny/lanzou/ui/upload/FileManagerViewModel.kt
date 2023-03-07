package cc.drny.lanzou.ui.upload

import android.os.Build
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class FileManagerViewModel(private val titles: Array<String>, private val paths: Array<String>) :
    ViewModel() {

    class FileManagerViewModelFactory(private val titles: Array<String>, private val paths: Array<String>) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FileManagerViewModel(titles, paths) as T
        }
    }

    private val _liveData = MutableLiveData<List<Fragment>>()
    val liveData: LiveData<List<Fragment>> get() = _liveData

    init {
        val fragments = mutableListOf<Fragment>()
        for (position in titles.indices) {
            fragments.add(if (position == 1) AppDataFragment()
            else if ((position == 2 || position == 3) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                AndroidDataFragment().apply {
                    arguments = bundleOf("path" to paths[position])
                }
            } else {
                LocalFileFragment().apply {
                    arguments = bundleOf("path" to paths[position], "name" to titles[position])
                }
            })
        }
        _liveData.value = fragments
    }
}