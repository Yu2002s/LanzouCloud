package cc.drny.lanzou.ui.upload

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.database.getStringOrNull
import androidx.lifecycle.*
import cc.drny.lanzou.LanzouApplication
import cc.drny.lanzou.R
import cc.drny.lanzou.data.upload.FileInfo
import cc.drny.lanzou.util.FileUtils.toSize
import cc.drny.lanzou.util.sortFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class UploadSelectorViewModel : ViewModel() {

    val selectedList = LinkedList<FileInfo>()

    fun addSelect(fileInfo: FileInfo) {
        selectedList.add(fileInfo)
    }

    fun addFirstSelect(fileInfo: FileInfo) {
        selectedList.add(0, fileInfo)
    }

    fun removeSelect(fileInfo: FileInfo) {
        selectedList.remove(fileInfo)
    }

    private lateinit var appLiveData: MutableLiveData<List<FileInfo>>
    private lateinit var imgLiveData: MutableLiveData<List<FileInfo>>
    private lateinit var fileLiveData: MutableLiveData<List<FileInfo>>
    private lateinit var zipLiveData: MutableLiveData<List<FileInfo>>
    private lateinit var apkLiveData: MutableLiveData<List<FileInfo>>
    private lateinit var videoLiveData: MutableLiveData<List<FileInfo>>
    private lateinit var documentLiveData: MutableLiveData<List<FileInfo>>

    suspend fun getApps(): LiveData<List<FileInfo>> {
        if (!::appLiveData.isInitialized) {
            appLiveData = MutableLiveData()
            val context = LanzouApplication.context
            val pm = context.packageManager
            val apps = withContext(Dispatchers.IO) {
                try {
                    val packages = pm.getInstalledPackages(PackageManager.GET_ACTIVITIES)
                    packages.sortedByDescending { it.lastUpdateTime }.map {
                        val app = it.applicationInfo
                        val name = app.loadLabel(pm).toString() + "-" + it.versionName + ".apk"
                        val file =  File(app.sourceDir)
                        FileInfo(
                            file.path.hashCode().toLong(), name, app.sourceDir, file.length()
                        ).apply {
                            extension = "apk"
                            fileDesc = fileLength.toSize() + " " + it.versionName
                            this.type = ClassificationFragment.TYPE_APP
                        }
                    }
                } catch (e: Exception) {
                    mutableListOf()
                }
            }
            appLiveData.value = apps
        }
        return appLiveData
    }

    suspend fun getFiles(): LiveData<List<FileInfo>> {
        if (!::fileLiveData.isInitialized) {
            fileLiveData = MutableLiveData()
            fileLiveData.value =
                query(MediaStore.Files.getContentUri("external"), ClassificationFragment.TYPE_FILE)
        }
        return fileLiveData
    }

    suspend fun getImages(): LiveData<List<FileInfo>> {
        if (!::imgLiveData.isInitialized) {
            imgLiveData = MutableLiveData()
            imgLiveData.value = query(Media.EXTERNAL_CONTENT_URI, ClassificationFragment.TYPE_IMG)
        }
        return imgLiveData
    }

    suspend fun getZips(context: Context): LiveData<List<FileInfo>> {
        if (!::zipLiveData.isInitialized) {
            zipLiveData = MutableLiveData()
            val type = arrayOf(
                "application/zip",
                "application/x-gtar",
                "application/x-gzip",
                "application/x-rar-compressed",
                "application/x-7z-compressed",
                "application/java-archive"
            )
            val zipIcon =
                ContextCompat.getDrawable(context, R.drawable.baseline_archive_24)
            zipLiveData.value = query(
                MediaStore.Files.getContentUri("external"),
                ClassificationFragment.TYPE_ZIP,
                type,
                zipIcon
            )
        }
        return zipLiveData
    }

    suspend fun getApks(): LiveData<List<FileInfo>> {
        if (!::apkLiveData.isInitialized) {
            apkLiveData = MutableLiveData()
            val type = "application/vnd.android.package-archive"
            apkLiveData.value = query(
                MediaStore.Files.getContentUri("external"),
                ClassificationFragment.TYPE_APK,
                arrayOf(type)
            )
        }
        return apkLiveData
    }

    suspend fun getVideos(): LiveData<List<FileInfo>> {
        if (!::videoLiveData.isInitialized) {
            videoLiveData = MutableLiveData()
            videoLiveData.value = query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, ClassificationFragment.TYPE_VIDEO
            )
        }
        return videoLiveData
    }

    suspend fun getDocument(context: Context): LiveData<List<FileInfo>> {
        if (!::documentLiveData.isInitialized) {
            documentLiveData = MutableLiveData()
            val mimeTypes = arrayOf(
                "application/rtf", "application/pdf", "application/msword"
            )
            documentLiveData.value = query(
                MediaStore.Files.getContentUri("external"),
                ClassificationFragment.TYPE_DOCUMENT,
                mimeTypes,
                ContextCompat.getDrawable(context, R.drawable.ic_doc)
            )
        }
        return documentLiveData
    }

    private suspend fun query(
        uri: Uri, type: Int = -1, mimeTypes: Array<String>? = null, icon: Drawable? = null
    ) = withContext(Dispatchers.IO) {
        val context = LanzouApplication.context
        val date = Media.DATE_MODIFIED
        val list = mutableListOf<FileInfo>()
        val selection =
            if (mimeTypes == null) "mime_type != ? and mime_type != ? and mime_type != ?"
            else {
                with(StringBuilder("mime_type = ?")) {
                    for (i in mimeTypes.indices) {
                        append(" or ")
                        append("mime_type = ?")
                    }
                    toString()
                }
            }
        val selectionArgs = mimeTypes ?: arrayOf("application/octet-stream", "null", "")
        val mimeTypeMap = MimeTypeMap.getSingleton()
        context.contentResolver.query(uri, null, selection, selectionArgs, "$date desc")?.apply {
            val nameColumn = getColumnIndex(Media.DISPLAY_NAME)
            val sizeColumn = getColumnIndex(Media.SIZE)
            val dataColumn = getColumnIndex(Media.DATA)
            val idColumn = getColumnIndex(Media._ID)
            val mimeColumn = getColumnIndex(Media.MIME_TYPE)
            while (moveToNext()) {
                val name = getStringOrNull(nameColumn)
                val size = getLong(sizeColumn)
                val path = getString(dataColumn)
                val id = getLong(idColumn)
                val mimeType = getString(mimeColumn)
                val extension = mimeTypeMap.getExtensionFromMimeType(mimeType)
                list.add(FileInfo(id, name ?: "null", path, size, size.toSize(), extension).apply {
                    this.icon = icon
                    this.type = type
                })
            }
            close()
        }
        list
    }

}