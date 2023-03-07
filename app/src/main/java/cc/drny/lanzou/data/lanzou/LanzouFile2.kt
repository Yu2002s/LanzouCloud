package cc.drny.lanzou.data.lanzou

import com.google.gson.annotations.SerializedName

data class LanzouFile2(
    val id: String,
    var name_all: String,
    val time: String,
    val size: String,
    @SerializedName("icon")
    var extension: String
)