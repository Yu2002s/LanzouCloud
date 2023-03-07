package cc.drny.lanzou.data.lanzou

import com.google.gson.annotations.SerializedName

data class LanzouUrlResponse(
    @SerializedName("zt")
    val status: Int,
    val info: LanzouUrl
)

data class LanzouUrl(
    val pwd: String? = null,
    @SerializedName("f_id")
    val fid: String,
    @SerializedName("onof")
    val hasPwd: Int,
    @SerializedName("is_newd")
    val host: String,
    @SerializedName("des")
    val describe: String,
    val name: String,
    @SerializedName("new_url")
    val url: String
)
