package cc.drny.lanzou.ui.user

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import cc.drny.lanzou.data.user.User
import cc.drny.lanzou.network.LanzouRepository

class LoginViewModel: ViewModel() {

    private val _liveData = MutableLiveData<User>()

    val liveData = Transformations.switchMap(_liveData) {
        LanzouRepository.login(it)
    }

    fun login(user: User) {
        _liveData.value = user
    }

}