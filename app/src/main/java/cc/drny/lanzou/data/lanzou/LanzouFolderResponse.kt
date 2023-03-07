package cc.drny.lanzou.data.lanzou

import com.google.gson.annotations.SerializedName

data class LanzouFolderResponse(
    @SerializedName("zt")
    val status: Int,
    @SerializedName("info")
    val folders: List<LanzouFolder>
)

data class LanzouFolder(
    val folder_id: Long,
    val folder_name: String
)
