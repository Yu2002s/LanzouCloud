package cc.drny.lanzou.event

interface UploadProgressListener {

    fun onProgress(current: Long, length: Long)

}