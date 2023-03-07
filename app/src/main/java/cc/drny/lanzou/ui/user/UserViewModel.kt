package cc.drny.lanzou.ui.user

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.drny.lanzou.LanzouApplication
import cc.drny.lanzou.network.LanzouRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class UserViewModel: ViewModel() {

    val userLiveData = MutableLiveData<Result<String>>()

    init {
        getUserData()
    }

    fun logout() {
        userLiveData.value = Result.failure(Throwable("未登录"))
    }

    fun getUserData() {
        viewModelScope.launch {
            try {
                val cookie = LanzouRepository.getUserCookie() ?: throw IllegalStateException("未登录")
                val document = withContext(Dispatchers.IO) {
                    Jsoup.connect(LanzouApplication.LANZOU_HOST_MYSELF)
                        .header("Cookie", cookie)
                        .get()
                }
                val element = document.selectFirst("div.c_topr")
                if (element == null) {
                    throw IllegalStateException("获取资料失败")
                } else {
                    var username = element.ownText()
                    val regex = Regex("\\d{11}")
                    if (regex.matches(username)) {
                       username =  username.substring(0, 3) + "****" + username.substring(7)
                    }
                    userLiveData.value = Result.success(username)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                userLiveData.value = Result.failure(e)
            }

        }
    }

}