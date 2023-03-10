package cc.drny.lanzou.base

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import cc.drny.lanzou.data.upload.Completed
import cc.drny.lanzou.data.upload.Error
import cc.drny.lanzou.data.upload.Insert
import cc.drny.lanzou.data.upload.Progress
import cc.drny.lanzou.data.upload.UploadState
import cc.drny.lanzou.event.UploadListener
import cc.drny.lanzou.event.UploadProgressListener
import cc.drny.lanzou.service.UploadService

open class BaseUploadFragment : BaseSuperFragment(), ServiceConnection, UploadListener {

    var onlyUpload = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireContext().bindService(
            Intent(requireContext(), UploadService::class.java),
            this,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().unbindService(this)
        uploadService?.uploadListener = null
        uploadService = null
    }

    var uploadService: UploadService? = null
        private set

    fun requireUploadService() = uploadService!!

    override fun onServiceConnected(name: ComponentName?, service: IBinder) {
        uploadService = (service as UploadService.UploadBinder).getService()
        if (onlyUpload) {
            return
        }
        uploadService?.uploadListener = this
    }

    override fun onServiceDisconnected(name: ComponentName?) {
    }

    override fun onUpload(uploadState: UploadState) {
    }

}