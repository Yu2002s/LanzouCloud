package cc.drny.lanzou.data.download

import org.litepal.crud.LitePalSupport

data class Thread(
    val downloadId: Long = -1,
    val index: Int = 0,
    var start: Long = 0,
    var end: Long = -1
): LitePalSupport() {

    val id: Long = 0

    fun update() {
        update(id)
    }

    fun isCompleted() = start >= end

}
