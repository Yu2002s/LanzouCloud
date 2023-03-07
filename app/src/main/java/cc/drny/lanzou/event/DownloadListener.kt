package cc.drny.lanzou.event

import cc.drny.lanzou.data.download.Download

interface DownloadListener {

    fun onDownload(download: Download, error: String? = null)

}