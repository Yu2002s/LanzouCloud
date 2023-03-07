package cc.drny.lanzou.data.lanzou

import com.google.gson.annotations.SerializedName

data class LanzouUploadResponse(
    @SerializedName("zt")
    val status: Int,
    val info: String,
    @SerializedName("text")
    val fileInfo: List<LanzouUploadInfo>
)

data class LanzouUploadInfo(
    val f_id: String,
    val name_all: String,
    val id: Long,
    val size: String
)