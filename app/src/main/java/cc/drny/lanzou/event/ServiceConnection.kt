package cc.drny.lanzou.event

import cc.drny.lanzou.service.UploadService

interface ServiceConnection {

    fun onServiceConnected(uploadService: UploadService)

}