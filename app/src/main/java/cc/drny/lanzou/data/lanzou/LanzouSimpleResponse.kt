package cc.drny.lanzou.data.lanzou

import com.google.gson.annotations.SerializedName

data class LanzouSimpleResponse(
    @SerializedName("zt")
    val status: Int,
    val info: String,
    val text: String
)
