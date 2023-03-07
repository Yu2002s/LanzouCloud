package cc.drny.lanzou.base

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import cc.drny.lanzou.event.DownloadListener
import cc.drny.lanzou.service.DownloadService

abstract class BaseDownloadFragment : BaseSuperFragment(), ServiceConnection, DownloadListener {

    var downloadService: DownloadService? = null

    fun requireDownloadService() = downloadService!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireContext().bindService(
            Intent(requireContext(), DownloadService::class.java),
            this,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().unbindService(this)
        downloadService?.downloadListener = null
        downloadService = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        downloadService = (service as DownloadService.DownloadBinder).getService()
        downloadService?.downloadListener = this
    }

    override fun onServiceDisconnected(name: ComponentName?) {
    }

}