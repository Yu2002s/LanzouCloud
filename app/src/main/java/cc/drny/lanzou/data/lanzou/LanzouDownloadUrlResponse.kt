package cc.drny.lanzou.data.lanzou

import com.google.gson.annotations.SerializedName

data class LanzouDownloadUrlResponse(
    @SerializedName("zt")
    val status: Int,
    val dom: String,
    val url: String,
    val inf: String
)