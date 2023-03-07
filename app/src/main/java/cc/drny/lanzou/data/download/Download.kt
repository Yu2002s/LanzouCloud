package cc.drny.lanzou.data.download

import android.graphics.drawable.Drawable
import org.litepal.annotation.Column
import org.litepal.crud.LitePalSupport

data class Download(
    val fileId: Long = -1,
    val name: String = "",
    val url: String = "",
    val pwd: String? = null,
    var path: String = "",
    var time: Long = System.currentTimeMillis(),
    var length: Long = -1,
    var extension: String? = null
) : LitePalSupport() {

    val id: Long = 0

    var progress: Int = -1

    var current: Long = 0

    @Column(ignore = true)
    var status: Int = STATUS_STOP

    @Column(ignore = true)
    var icon: Drawable? = null

    @Column(ignore = true)
    var isSelected = false

    fun update() {
        update(id)
    }

    fun isCompleted() = progress == 100

    fun isPrepare() = status == STATUS_PREPARE

    fun prepare() {
        status = STATUS_PREPARE
    }

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
    }

    fun download() {
        status = STATUS_PROGRESS
    }

    fun isDownloading() = status < STATUS_STOP

    fun isInsert() = status == STATUS_INSERT

    fun isStop() = !isDownloading()

    fun getStatusStr() = if (isCompleted()) "已下载" else when (status) {
        STATUS_INSERT -> "已加入队列"
        STATUS_PREPARE -> "准备中..."
        STATUS_PROGRESS -> "下载中..."
        STATUS_STOP -> "已停止"
        STATUS_ERROR -> "下载错误"
        STATUS_COMPLETED -> "已下载"
        else -> null
    }

    override fun toString(): String {
        return "Download(fileId=$fileId, name='$name', url='$url', pwd=$pwd, path='$path', time=$time, length=$length, extension=$extension, id=$id, progress=$progress, current=$current, status=$status, icon=$icon)"
    }


    companion object {
        const val STATUS_PREPARE = -1
        const val STATUS_INSERT = 0
        const val STATUS_PROGRESS = 1
        const val STATUS_STOP = 2
        const val STATUS_ERROR = 3
        const val STATUS_COMPLETED = 4

    }

}
