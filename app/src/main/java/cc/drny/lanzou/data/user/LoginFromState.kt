package cc.drny.lanzou.data.user

import com.google.gson.annotations.SerializedName

data class LoginFromState(
    @SerializedName("zt")
    val status: Int,
    val info: String
)