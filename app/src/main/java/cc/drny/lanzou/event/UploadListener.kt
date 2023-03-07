package cc.drny.lanzou.event

import cc.drny.lanzou.data.upload.Upload
import cc.drny.lanzou.data.upload.UploadState

interface UploadListener {

    fun onUpload(uploadState: UploadState)

}