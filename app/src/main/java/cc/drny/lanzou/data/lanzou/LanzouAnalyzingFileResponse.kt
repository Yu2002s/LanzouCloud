package cc.drny.lanzou.data.lanzou

import com.google.gson.annotations.SerializedName

data class LanzouAnalyzingFileResponse(
    @SerializedName("zt")
    val status: Int,
    @SerializedName("text")
    val files: List<LanzouFile2>,
    val info: String
)
