package cc.drny.lanzou.data.upload

import android.graphics.drawable.Drawable
import org.litepal.annotation.Column
import org.litepal.crud.LitePalSupport

data class Upload(
    var folderId: Long = -1,
    var folderName: String = "",
    val name: String = "",
    var path: String = "",
    var encryptId: String = "",
    @Column(index = true)
    var fileId: Long = -1,
    var current: Long = -1,
    var length: Long = -1,
    var time: Long = 0,
    var extension: String? = null
) : LitePalSupport() {

    companion object {
        const val STATUS_INSERT = 0
        const val STATUS_PROGRESS = 1
        const val STATUS_STOP = 2
        const val STATUS_ERROR = 3
        const val STATUS_COMPLETED = 4

    }

    val id: Long = 0
    var progress: Int = -1

    @Column(ignore = true)
    var status: Int = STATUS_STOP

    @Column(ignore = true)
    var icon: Drawable? = null

    @Column(ignore = true)
    var isSelected = false

    fun update() = update(id)

    fun insert() {
        status = STATUS_INSERT
    }

    fun progress() {
        status = STATUS_PROGRESS
    }

    fun stop() {
        status = STATUS_STOP
    }

    fun error() {
        status = STATUS_ERROR
    }

    fun completed() {
        status = STATUS_COMPLETED
        progress = 100
    }

    fun isUploading() = status <= STATUS_PROGRESS

    fun isCompleted() = progress == 100

    fun getStatusStr() =
        if (isCompleted()) "已上传" else
            when (status) {
                STATUS_INSERT -> "准备中..."
                STATUS_PROGRESS -> "上传中..."
                STATUS_STOP -> "已停止"
                STATUS_ERROR -> "上传出错"
                STATUS_COMPLETED -> "已上传"
                else -> ""
            }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Upload) return false

        if (id != other.id) return false
        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }
}
