package cc.drny.lanzou.data.lanzou

import com.google.gson.annotations.SerializedName

data class LanzouFileResponse(
    @SerializedName("zt")
    val status: Int,
    @SerializedName("text")
    val files: List<LanzouFile>
)
