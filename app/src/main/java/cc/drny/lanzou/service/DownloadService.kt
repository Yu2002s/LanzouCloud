package cc.drny.lanzou.service

import android.app.Service
import android.app.backup.SharedPreferencesBackupHelper
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.preference.PreferenceManager
import cc.drny.lanzou.data.download.Download
import cc.drny.lanzou.data.download.Thread
import cc.drny.lanzou.data.lanzou.LanzouFile
import cc.drny.lanzou.data.lanzou.LanzouShareFile
import cc.drny.lanzou.event.DownloadListener
import cc.drny.lanzou.network.HttpParam
import cc.drny.lanzou.network.LanzouRepository
import cc.drny.lanzou.util.getIconForExtension
import cc.drny.lanzou.util.openFile
import cc.drny.lanzou.util.showToast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.notifyAll
import okhttp3.internal.wait
import org.litepal.LitePal
import org.litepal.extension.deleteAll
import java.io.File
import java.io.RandomAccessFile

/**
 * 下载文件服务
 */
class DownloadService : Service() {

    companion object {
        /**
         * 下载线程数
         */
        private const val THREAD_COUNT = 3
    }

    private var downloadPath = Environment
        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path

    var downloadListener: DownloadListener? = null

    /**
     * 下载作用域
     */
    private val downloadCoroutineScope = CoroutineScope(Dispatchers.IO)

    private val downloadJobs = linkedMapOf<Long, Job>()
    private val okHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .addInterceptor {
            val request = it.request()
            val response = it.proceed(request)
            val location = response.header("Location")
            if (location != null) {
                response.close()
                it.proceed(
                    request.newBuilder()
                        .url(location)
                        .build()
                )
            } else {
                response
            }
        }
        .build()

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    inner class DownloadBinder : Binder() {
        fun getService() = this@DownloadService
    }

    override fun onBind(intent: Intent?): IBinder {
        return DownloadBinder()
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        editor = sharedPreferences.edit()
    }

    suspend fun deleteDownload(download: Download, block: () -> Unit) {
        try {
            downloadJobs[download.fileId]?.cancel()
            downloadJobs.remove(download.fileId)
            withContext(Dispatchers.IO) {
                download.delete()
                LitePal.deleteAll<Thread>("downloadId = ?", download.id.toString())
                File(download.path).delete()
            }
            // 删除成功
            block.invoke()
        } catch (_: Exception) {
        }
    }

    fun switchDownload(download: Download) {
        if (download.isCompleted()) return
        // 如果当前是下载状态
        if (download.isDownloading()) {
            // 执行停止下载
            download.stop()
            Log.d("jdy", "pauseDownload")
            // downloadJobs[download.fileId]?.cancel()
            downloadListener?.onDownload(download)
        } else {
            // 继续下载
            val downloadJob = downloadJobs[download.fileId]
            if (downloadJob != null) {
                // 说明下载线程是在暂停状态
                download.download()
                synchronized(download) {
                    download.notifyAll()
                }
                Log.d("jdy", "resumeDownload")
                return
            }
            Log.d("jdy", "reDownload")
            // 停止状态，需要重新进行下载
            downloadJobs[download.fileId] = downloadCoroutineScope.launch {
                try {
                    startDownloadFile(download)
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        return@launch
                    }
                    download.error()
                    updateDownloadStatus(download, e.message)
                } finally {
                    // 结束
                    download.update()
                    downloadJobs.remove(download.fileId)
                }
            }
        }
    }

    /**
     * 添加下载任务
     * @param lanzouFile 需要下载的文件
     * @param block 下载回调
     */
    fun addDownload(
        lanzouFile: LanzouFile,
        lanzouShareFile: LanzouShareFile? = null,
        block: suspend (Download) -> Unit
    ) {
        val fileId = lanzouFile.fileId
        val name = lanzouFile.name_all
        if (downloadJobs.containsKey(fileId)) {
            // 下载任务已存在
            Log.d("jdy", "下载任务已存在")
            "${lanzouFile.name_all}下载任务已存在".showToast()
            return
        }
        downloadJobs[fileId] = downloadCoroutineScope.launch {
            var download: Download? = null
            try {
                download = LitePal.where("fileId = ?", fileId.toString())
                    .findFirst(Download::class.java)
                if (download == null) {
                    download = if (lanzouShareFile == null) getDownloadInfo(fileId, name) else {
                        val path = getExternalFilesDir("Download")!!.path + "/$name"
                        Download(
                            fileId,
                            name,
                            lanzouShareFile.url,
                            lanzouShareFile.pwd,
                            path
                        )
                    }.also {
                        it.extension = lanzouFile.icon ?: lanzouShareFile?.extension
                    }
                    download.icon = lanzouFile.iconDrawable ?: lanzouShareFile?.icon

                    download.insert()
                    withContext(Dispatchers.Main) {
                        // 加入下载队列时进行回调
                        block.invoke(download)
                    }

                    // 这里可以加载一下对应的图标
                    download.saveThrows()
                    withContext(Dispatchers.Main) {
                        (download.name + "已加入下载队列").showToast()
                        downloadListener?.onDownload(download)
                    }
                }
                Log.d("jdy", "downloadStart: $download")
                startDownloadFile(download, lanzouShareFile?.downloadUrl)
            } catch (e: Throwable) {
                e.printStackTrace()
                // 处理异常了
                if (e is CancellationException) {
                    // 协程取消时不进行处理
                    return@launch
                }
                download?.let {
                    it.error()
                    updateDownloadStatus(it, e.message)
                }
                // 出错处理
            } finally {
                Log.d("jdy", "finish download")
                // 下载结束了
                download?.update()
                downloadJobs.remove(fileId)
            }
        }
    }

    private suspend fun getDownloadInfo(fileId: Long, name: String): Download {
        val fileInfo = LanzouRepository.getFileInfo(fileId).getOrThrow()
        val url = LanzouRepository.getShareUrl(fileInfo)

        val path = getExternalFilesDir("Download")!!.path + "/$name"
        val pwd = if (fileInfo.hasPwd == 1) fileInfo.pwd else null
        return Download(fileId, name, url, pwd, path)
    }

    /**
     * 通过下载链接进行下载文件
     */
    private suspend fun startDownloadFile(download: Download, url: String? = null) {
        // 开始下载文件
        if (download.isCompleted()) {
            // 重复下载
            download.completed()
            updateDownloadStatus(download)
            download.path.openFile()
            Log.d("jdy", "下载已完成，无需继续下载")
            return
        }
        coroutineScope {
            val downloadUrl =
                url ?: LanzouRepository.getDownloadUrl(download.url, download.pwd).getOrThrow()
            val response = getResponse(downloadUrl)
            val isRange = response.header("Content-Range") != null
            Log.d("jdy", "isRange: $isRange")
            if (!isRange && download.progress != 0) {
                download.progress = 0
                download.current = 0
            }
            download.length =
                response.body?.contentLength() ?: throw IllegalStateException("获取文件大小出错")
            response.close()
            if (download.length <= 0) throw IllegalStateException("获取大小出错了")
            // 更新下载状态
            // 准备中...
            download.prepare()
            updateDownloadStatus(download)
            // 查询当前下载所需的线程
            val threads =
                LitePal.where("downloadId = ?", download.id.toString()).find(Thread::class.java)
            // 第一次进行下载线程是空的
            if (threads.isEmpty()) {
                if (isRange) {
                    val threadCount = sharedPreferences.getInt("thread_count", THREAD_COUNT)
                    val threadSize =
                        if (download.length % threadCount == 0L) download.length / threadCount
                        else download.length / threadCount + 1
                    for (i in 0 until threadCount) {
                        val start = i * threadSize
                        val end =
                            if (i != threadCount - 1) (i + 1) * threadSize - 1 else download.length - 1
                        val thread = Thread(download.id, i, start, end)
                        threads.add(thread)
                        thread.saveThrows()
                    }
                } else {
                    val thread = Thread(download.id, 0, 0, download.length)
                    thread.saveThrows()
                    threads.add(thread)
                }
            }

            // 开始下载了
            download.progress()
            updateDownloadStatus(download)

            val list = mutableListOf<Deferred<Unit>>()
            threads.forEach { thread ->
                if (!isRange && thread.start != 0L) {
                    thread.start = 0
                }
                Log.d("jdy", "thread: $thread")
                val result = async {
                    writeData(downloadUrl, download, thread)
                }
                list.add(result)
            }
            // 并行多线程执行下载
            list.forEach {
                it.await()
            }

            // 这里下载完成了
            Log.d("jdy", "downloadCompleted: $download")
            if (download.isCompleted()) {
                val source = File(download.path)
                downloadPath = sharedPreferences.getString("download_path", downloadPath)!!
                val downloadFile = File(downloadPath)
                if (!downloadFile.exists()) {
                    downloadFile.mkdirs()
                }
                val target = File(downloadFile, download.name)
                if (source.renameTo(target)) {
                    // 移动文件成功了
                    download.path = target.path
                }
                // 重命名失败了，可能是未被授权权限

                download.icon =
                    download.path.getIconForExtension(this@DownloadService, download.extension)
                download.completed()
                updateDownloadStatus(download)
                val autoInstall = sharedPreferences.getBoolean("auto_install", true)
                if (download.extension == "apk" && autoInstall) {
                    target.openFile()
                }
            } else throw IllegalStateException("下载出错了")
        }
    }

    /**
     * 开始写入数据到本地
     */
    private suspend fun writeData(downloadUrl: String, download: Download, thread: Thread) {
        withContext(Dispatchers.IO) {
            Log.d("jdy", "thread: " + java.lang.Thread.currentThread().name)
            val randomAccessFile = RandomAccessFile(download.path, "rwd")
            randomAccessFile.setLength(download.length)
            randomAccessFile.seek(thread.start)
            val request = Request.Builder()
                .url(downloadUrl)
                .headers(HttpParam.httpHeaders)
                .addHeader("Range", "bytes=${thread.start}-${thread.end}")
                .build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body ?: throw IllegalStateException("body is null")

            if (body.contentLength() <= 0) throw IllegalStateException("获取文件出错")

            Log.d("jdy", "contentLength: " + body.contentLength())
            val inputStream = body.byteStream()
            val bytes = ByteArray(8 * 1024)
            var len = inputStream.read(bytes)
            var currentProgress = 0
            while (len != -1) {
                randomAccessFile.write(bytes, 0, len)
                synchronized(download) {
                    thread.start += len
                    download.current += len
                    if (download.isStop()) {
                        Log.d("jdy", "waitDownload")
                        download.wait()
                    }
                }
                download.progress = (download.current * 100 / download.length).toInt()
                // 进度加2执行数据库刷新操作
                if (download.progress - currentProgress >= 2) {
                    currentProgress = download.progress
                    thread.update()
                    download.update()
                    updateDownloadStatus(download)
                }
                len = inputStream.read(bytes)
            }
            // 线程执行完成时，删除指定线程
            if (thread.isCompleted())
                thread.delete()
            response.close()
        }
    }

    private fun getResponse(url: String): Response {
        val request = Request.Builder().url(url)
            .headers(HttpParam.httpHeaders)
            .addHeader("Range", "bytes=0-")
            .build()
        return okHttpClient.newCall(request).execute()
    }

    private suspend fun updateDownloadStatus(download: Download, error: String? = null) {
        downloadListener?.let {
            withContext(Dispatchers.Main) {
                it.onDownload(download, error)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadCoroutineScope.cancel()
    }

}