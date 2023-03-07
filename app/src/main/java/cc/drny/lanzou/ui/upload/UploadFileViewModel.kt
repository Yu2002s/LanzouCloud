package cc.drny.lanzou.ui.upload

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UploadFileViewModel: ViewModel() {

    private val _liveData = MutableLiveData<List<Fragment>>()
    val liveData: LiveData<List<Fragment>> get() = _liveData

    init {
        val fragments = mutableListOf<Fragment>()
        fragments.add(ClassificationFragment())
        fragments.add(FileManagerFragment())
        fragments.add(FileSearchFragment())
        Log.d("jdy", fragments.toString())
        _liveData.value = fragments

        // [ClassificationFragment{fccf53a} (68468eeb-2277-4bf2-8a1f-03016d77c86d), FileManagerFragment{1d845eb} (7d90cff6-381b-46b3-874c-f4bd739d9843), FileSearchFragment{d258f48} (6d43adba-7984-4a8e-854f-00e873c69ccf)]
    }

}