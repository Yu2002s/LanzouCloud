package cc.drny.lanzou.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.ArrayMap
import android.util.Log
import androidx.core.net.toUri
import cc.drny.lanzou.R
import cc.drny.lanzou.data.upload.*
import cc.drny.lanzou.event.UploadListener
import cc.drny.lanzou.event.UploadProgressListener
import cc.drny.lanzou.network.LanzouRepository
import cc.drny.lanzou.util.ApplySigningUtils
import cc.drny.lanzou.util.showToast
import cc.drny.lanzou.util.toFile
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.*
import org.litepal.LitePal
import org.litepal.extension.findFirst
import java.io.File
import java.io.IOException
import kotlin.system.exitProcess

class UploadService : Service() {

    companion object {
        /**
         * 上传文件存放的目录名称
         */
        private const val UPLOAD_DIR = "Upload"
        private const val KEY_UPLOAD_DIR_ID = "upload_dir_id"

        const val APP_SIGNATURE = "2f227d414a2533eb4c907475a728bad5"
    }

    /**
     * 上传文件协程作用域
     */
    private val uploadCoroutineScope = CoroutineScope(Dispatchers.Main)

    var uploadListener: UploadListener? = null

    private val mHandler = Handler(Looper.getMainLooper())

    private val mmkv = MMKV.defaultMMKV()

    /**
     * 所有上传文件队列 ID对应一个上传队列
     */
    private val uploadList = ArrayMap<Long, Job>()

    inner class UploadBinder : Binder() {
        fun getService() = this@UploadService
    }

    override fun onBind(intent: Intent?): IBinder {
        return UploadBinder()
    }

    override fun onCreate() {
        super.onCreate()
        // 检查软件签名是否被修改
        val signatureStr = ApplySigningUtils.getRawSignatureStr(this, packageName)
        if (signatureStr != APP_SIGNATURE) {
            exitProcess(0)
        }
    }

    /**
     * 切换停止或者继续上传
     */
    fun switchUpload(upload: Upload) {
        if (upload.isUploading()) {
            // 暂停上传
            upload.stop()
            uploadList[upload.fileId]?.cancel()
            // uploadList.remove(upload.fileId)
            uploadListener?.onUpload(Stop(upload))
            Log.d("jdy", "stopUpload")
        } else {
            Log.d("jdy", "resumeUpload")
            // 继续上传
            uploadList[upload.fileId] = uploadCoroutineScope.launch(Dispatchers.IO) {
                try {
                    upload.time = System.currentTimeMillis()
                    startUpload(upload)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("jdy", e.toString())
                    if (e !is CancellationException) {
                        upload.error()
                        withContext(Dispatchers.Main) {
                            uploadListener?.onUpload(Error(upload, e.message))
                        }
                    }
                } finally {
                    upload.update()
                    uploadList.remove(upload.fileId)
                }
            }

        }
    }

    suspend fun deleteUpload(upload: Upload): Boolean {
        return try {
            uploadList[upload.fileId]?.cancel()
            uploadList.remove(upload.fileId)
            withContext(Dispatchers.IO) {
                upload.delete()
                val file = File(upload.path)
                if (file.parent == externalCacheDir!!.path) {
                    file.delete()
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    inner class UploadRunnable(private val progress: Progress) : Runnable {
        override fun run() {
            uploadListener?.onUpload(progress)
        }
    }

    /**
     * 开始进行上传了
     */
    private suspend fun startUpload(upload: Upload) {
        val path = upload.path
        val isUri = path.startsWith("content://")
        val file = if (isUri) {
            upload.path.toUri().toFile() ?: throw IllegalStateException("文件上传出错")
        } else File(upload.path)
        // 标记正在上传了
        upload.progress()

        val uploadResponse = LanzouRepository.uploadFile(upload.folderId, file, upload.name,
            object : UploadProgressListener {

                private val progress = Progress(upload)
                private val uploadRunnable = UploadRunnable(progress)
                private var currentProgress = 0

                override fun onProgress(current: Long, length: Long) {
                    upload.current = current
                    upload.progress = (current * 100 / length).toInt()
                    if (upload.progress - currentProgress >= 2) {
                        currentProgress = upload.progress
                        upload.update()
                    }
                    if (upload.isCompleted()) {
                        mHandler.removeCallbacks(uploadRunnable)
                    } else {
                        mHandler.post(uploadRunnable)
                    }
                }
            })
        if (uploadResponse.status == 1) {
            // 上传成功了
            val uploadInfo = uploadResponse.fileInfo[0]
            upload.fileId = uploadInfo.id
            upload.encryptId = uploadInfo.f_id
            upload.completed()
            withContext(Dispatchers.Main) {
                (upload.name + "上传完成").showToast()
                uploadListener?.onUpload(Completed(upload))
            }
        } else {
            throw IllegalStateException("上传出错了")
        }
        /*if (isUri) {
            file.delete()
        }*/
    }

    /**
     * 上传文件
     * @param fileInfo 文件的上传信息
     * @param folderId 上传到指定文件夹ID
     * @param folderName 上传文件夹名称
     * @param block 文件添加到上传队列的回调
     */
    fun addUpload(
        fileInfo: FileInfo,
        folderId: Long,
        folderName: String,
        block: suspend (Upload) -> Unit
    ) {
        val id = fileInfo.id
        if (uploadList.containsKey(id)) {
            // 队列已存在了
            Log.d("jdy", "任务已存在")
            return
        }
        uploadList[id] = uploadCoroutineScope.launch(Dispatchers.IO) {
            // 通过文件ID获取该文件是否在上传队列中
            var upload: Upload? = null
            try {
                upload = LitePal.where("fileId = ?", id.toString()).findFirst<Upload>()
                if (upload == null) {
                    // 这里确定是第一次进行上传文件，文件不存在上传队列中
                    upload = Upload(folderId, folderName, fileInfo.name, fileInfo.path)
                    upload.length = fileInfo.fileLength
                    upload.fileId = id
                    upload.time = System.currentTimeMillis()
                    upload.extension = fileInfo.extension

                    upload.insert()
                    withContext(Dispatchers.Main) {
                        block.invoke(upload)
                    }
                    Log.d("jdy", "fileInfo: $fileInfo")

                    upload.saveThrows()

                    withContext(Dispatchers.Main) {
                        (upload.name +"已加入上传队列").showToast()
                        // 这里执行插入回调
                        uploadListener?.onUpload(Insert(upload))
                    }
                } else {
                    // 文件已在上传队列中
                    // 这里直接执行开始上传
                }

                startUpload(upload)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("jdy", e.toString())
                if (upload == null) return@launch
                if (e !is CancellationException) {
                    upload.error()
                    withContext(Dispatchers.Main) {
                        uploadListener?.onUpload(Error(upload, e.message))
                    }
                }
            } finally {
                // 这里总是会执行到
                // 这里进行最后的数据更新
                upload?.update()
                // 移除任务栈
                val job = uploadList.remove(id)
                Log.d("jdy", "always: " + job)
            }
        }

    }

    /**
     * 检查是否存在换粗文件夹在根目录中
     */
    private suspend fun isExistCacheDir(): Boolean {
        /*return LanzouRepository.get
            .map { it.name }
            .contains(UPLOAD_DIR)*/
        return false
    }

    /**
     * 检查上传文件夹是否已创建,未创建则进行创建
     */
    private fun checkUploadDir() {
        uploadCoroutineScope.launch(Dispatchers.IO) {
            if (!isExistCacheDir()) {
                // 未创建则需要进行创建文件夹
                LanzouRepository.createFolder(-1, UPLOAD_DIR, getString(R.string.upload_dir_desc))
                    .onFailure {
                        withContext(Dispatchers.Main) {
                            ("致命错误: " + it.message).showToast()
                        }
                    }.onSuccess {
                        mmkv.encode(KEY_UPLOAD_DIR_ID, it)
                    }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        uploadCoroutineScope.cancel()
        mHandler.removeCallbacksAndMessages(null)
    }
}