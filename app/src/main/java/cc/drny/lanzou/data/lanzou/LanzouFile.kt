package cc.drny.lanzou.data.lanzou

import android.graphics.drawable.Drawable
import com.google.gson.annotations.SerializedName

data class LanzouFile(
    var name: String = "",
    var name_all: String = "",
    @SerializedName("fol_id")
    var folderId: Long = 0,
    @SerializedName("id")
    var fileId: Long = 0,
    var icon: String? = null,
    var time: String = "",
    var size: String = "",
    @SerializedName("downs")
    val downloadCount: Int = 0,
    @SerializedName("folder_des")
    var describe: String? = null,
    var fid: String = ""
) {

    var iconDrawable: Drawable? = null

    var isSelected = false

    fun isFolder() = folderId != 0L

    fun isFile() = fileId != 0L

    fun getFileName() = if (isFile() || name_all.isNotEmpty()) name_all else name

    fun getId() = if (isFile()) fileId else folderId

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        other as LanzouFile

        if (name != other.name) return false
        if (folderId != other.folderId) return false
        if (fileId != other.fileId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + folderId.hashCode()
        result = 31 * result + fileId.hashCode()
        return result
    }


}