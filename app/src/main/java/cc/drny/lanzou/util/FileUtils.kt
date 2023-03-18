package cc.drny.lanzou.util

import android.content.*
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.os.Parcelable
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.provider.DocumentsContractCompat
import androidx.documentfile.provider.DocumentFile
import cc.drny.lanzou.LanzouApplication
import cc.drny.lanzou.R
import cc.drny.lanzou.data.upload.FileInfo
import cc.drny.lanzou.network.LanzouRepository
import cc.drny.lanzou.util.FileUtils.toSize
import com.bumptech.glide.Glide
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.util.ArrayList


private const val MEDIA_FILE_URI = "com.android.providers.media.documents"
private const val EXTERNAL_FILE_URI = "com.android.externalstorage.documents"
private const val DOWNLOAD_FILE_URI = "com.android.providers.downloads.documents"

object FileUtils {

    private val decimalFormat = DecimalFormat("0.00")

    fun Long.toSize(): String {
        return when {
            this < 1024 -> this.toString() + "B"
            this < 1048576 -> decimalFormat.format(this / 1024f) + "KB"
            this < 1073741824 -> decimalFormat.format(this / 1048576f) + "MB"
            else -> decimalFormat.format(this / 1.07374182E9f) + "GB"
        }
    }
}

fun String?.getAppIcon(context: Context): Drawable {
    if (this == null || startsWith("content://")) {
        return ContextCompat.getDrawable(context, R.drawable.ic_apk)!!
    }

    return try {
        val pm = context.packageManager
        val packageArchiveInfo = pm.getPackageArchiveInfo(this, 0) ?: throw NullPointerException()
        val applicationInfo = packageArchiveInfo.applicationInfo
        applicationInfo.sourceDir = this
        applicationInfo.publicSourceDir = this
        applicationInfo.loadIcon(pm)
    } catch (e: Exception) {
        ContextCompat.getDrawable(context, R.drawable.ic_apk)!!
    }

}

fun String?.getImageIcon(context: Context, ext: String?): Drawable {
    if (this == null) {
        ContextCompat.getDrawable(context, R.drawable.ic_file)!!
    }
    return try {
        Glide.with(context)
            .load(this)
            //.override(100)
            .submit(100, 100)
            .get()
    } catch (e: Exception) {
        ext.getIcon(context)
        // ContextCompat.getDrawable(context, R.drawable.ic_file)!!
    }
}

fun String?.getIconForExtension(context: Context, ext: String?): Drawable {
    return when (ext) {
        "apk" -> getAppIcon(context)
        "jpg", "png", "jpeg", "webp", "gif", "mp4" -> getImageIcon(context, ext)
        else -> ext.getIcon(context)
    }
}

/**
 * 获取缩率图
 */
fun String?.getIcon(context: Context): Drawable {
    val resId = when (this) {
        "apk", "apks" -> R.drawable.ic_apk
        "jpg", "jpeg" -> R.drawable.ic_jpg
        "png" -> R.drawable.ic_png
        "webp" -> R.drawable.ic_image
        "gif" -> R.drawable.ic_gif
        "zip" -> R.drawable.ic_zip
        "rar" -> R.drawable.ic_rar
        "jar" -> R.drawable.ic_jar
        "tar", "tgz" -> R.drawable.ic_archive
        "7z" -> R.drawable.ic_7z
        "mp4" -> R.drawable.ic_mp4
        "avi" -> R.drawable.ic_video
        "mp3" -> R.drawable.ic_mp3
        "flac" -> R.drawable.ic_flac
        "exe" -> R.drawable.ic_exe
        "txt" -> R.drawable.ic_txt
        "pdf" -> R.drawable.ic_pdf
        "doc", "docx" -> R.drawable.ic_word
        "ppt" -> R.drawable.ic_ppt
        "xls", "xlsx" -> R.drawable.ic_xls
        "md" -> R.drawable.ic_md
        "xml" -> R.drawable.ic_xml
        "html" -> R.drawable.ic_html
        "svg" -> R.drawable.ic_svg
        "json" -> R.drawable.ic_json
        "bak" -> R.drawable.ic_backup
        "tmp", "temp" -> R.drawable.ic_temp
        "xmind" -> R.drawable.ic_xmind
        "psd" -> R.drawable.ic_psd
        "ai" -> R.drawable.ic_ai
        "aac" -> R.drawable.ic_aac
        "sh" -> R.drawable.ic_shell
        "img", "iso" -> R.drawable.ic_img
        "java" -> R.drawable.ic_java
        "kt" -> R.drawable.ic_kotlin
        else -> R.drawable.ic_file
    }
    return ContextCompat.getDrawable(context, resId)!!
}

fun File.openFile(): Boolean {
    if (!this.exists()) {
        return false
    }
    val intent = this.getIntent()
    LanzouApplication.context.startActivity(intent)
    return true
}

fun String.getUploadPath(name: String): String {
    return if (startsWith("content://")) {
        LanzouApplication.context.externalCacheDir!!.path + "/$name"
    } else {
        this
    }
}

fun String.getUploadFile(name: String): File {
    return File(getUploadPath(name))
}

/**
 *  通过文件路径打开一个文件
 */
fun String.openFile(): Boolean {
    return File(this).openFile()
}

fun File.getShareIntent(title: String = "分享到"): Intent {
    return Intent.createChooser(getIntent(Intent.ACTION_SEND), title)
}

fun File.getIntent(action: String = Intent.ACTION_VIEW): Intent {
    val mimeType = MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(extension) ?: "*/*"
    val intent = Intent(action)
    intent.addCategory(Intent.CATEGORY_DEFAULT)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.setDataAndType(getUri(), mimeType)
    return intent
}

fun String.getTextIntent(): Intent {
    val intent = Intent(Intent.ACTION_SEND)
    intent.putExtra(Intent.EXTRA_TEXT, this)
    intent.type = "text/plain"
    return intent
}

fun List<File>.getIntent(action: String = Intent.ACTION_SEND_MULTIPLE): Intent {
    val intent = Intent(action)
    intent.addCategory(Intent.CATEGORY_DEFAULT)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    val uris = map { it.getUri() }
    intent.type = "*/*"
    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris as ArrayList<out Parcelable>)
    return intent
}

fun File.getUri(): Uri {
    return FileProvider.getUriForFile(
        LanzouApplication.context,
        "cc.drny.lanzou.fileProvider", this
    )
}

fun Intent.getUris(): List<Uri>? {
    if (data == null && clipData == null && clipData?.itemCount == 0) {
        return null
    }
    val uris = mutableListOf<Uri>()
    if (data != null) {
        val uri = data!!
        uris.add(uri)
    } else if (clipData != null) {
        val datas = clipData!!
        for (i in 0 until datas.itemCount) {
            val data = datas.getItemAt(i) ?: continue
            if (data.uri == null) continue
            uris.add(data.uri)
        }
    }
    return uris
}

fun Intent.getFiles(): List<FileInfo>? {
    val uris = getUris() ?: return null
    val files = mutableListOf<FileInfo>()
    uris.forEach { uri ->
        val absolutePath = uri.getFileAbsolutePath()
        if (absolutePath != null) {
            val file = File(absolutePath)
            if (file.isFile) {
                files.add(file.toFileInfo())
            } else {
                file.listFiles()?.forEach { child ->
                    if (child.isFile) {
                        files.add(child.toFileInfo())
                    }
                }
            }
        }
    }
    return files
}

fun File.toFileInfo(): FileInfo {
    val length = length()
    return FileInfo(
        path.hashCode().toLong(),
        name,
        path,
        length,
        length.toSize(),
        extension
    ).also { it.isSelected = true }
}

fun Uri?.getFileAbsolutePath(): String? {
    if (this == null) {
        return null
    }
    if ("file".equals(scheme, true)) {
        return path
    }
    return try {
        when (authority) {
            EXTERNAL_FILE_URI -> {
                val docId = DocumentsContractCompat.getDocumentId(this) ?: return null
                val split = docId.split(":")
                val type = split[0]
                if ("primary".equals(type, true)) {
                    return Environment.getExternalStorageDirectory().path + "/" + split[1]
                }
                return null
            }
            DOWNLOAD_FILE_URI -> {
                val id = DocumentsContractCompat.getDocumentId(this) ?: return null
                val ids = id.split(":")
                if (ids.size > 1) {
                    throw IllegalStateException()
                    /*val documentFile =
                        DocumentFile.fromSingleUri(LanzouApplication.context, this) ?: return null
                    val fileName = documentFile.name ?: return null
                    return null//this.toFile("", if (fileName.canUpload()) "" else ".apk")!!.path*/
                }
                val cursor = LanzouApplication.context.contentResolver
                    .query(this, null, null, null, null) ?: return null
                if (cursor.count == 0) {
                    cursor.close()
                    return null
                }
                cursor.moveToFirst()
                val name = cursor.getString(cursor.getColumnIndexOrThrow("_display_name"))
                cursor.close()
                return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + "/$name"
            }
            MEDIA_FILE_URI -> {
                val docId = DocumentsContract.getDocumentId(this) ?: return null
                val split = docId.split(":")
                val contentUri = when (split[0]) {
                    "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    else -> MediaStore.Files.getContentUri("external")
                }
                LanzouApplication.context.contentResolver
                    .query(contentUri, null, "_id = ?", arrayOf(split[1]), null)?.apply {
                        if (moveToFirst()) {
                            return getString(getColumnIndexOrThrow("_data"))
                        }
                        close()
                    }
                return null
            }
            else -> getFilePath()
        }
    } catch (e: Exception) {
        if (path != null) {
            val file = File(path!!)
            return if (file.exists()) {
                file.path
            } else {
                toFile()?.path
            }
        }
        null
    }
}

/**
 *  通过 Uri 得到文件名
 *  @param context 上下文
 */
fun Uri.getFileName(): String {
    var fileName = ""
    if (scheme == ContentResolver.SCHEME_FILE) fileName = lastPathSegment ?: return fileName
    else {
        LanzouApplication.context.contentResolver.query(this, null, null, null, null)
            ?.apply {
                if (count > 0) {
                    moveToFirst()
                    fileName =
                        getString(getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                }
                close()
            }
    }
    if (fileName.isEmpty()) {
        fileName = System.currentTimeMillis().toString();
    }
    return fileName
}

fun Uri.toFile(): File? {
    return toFile("")
}

/**
 *  Android11 Uri转文件(图片可进行压缩)
 *  @param pathName 保存到的外部私有目录.
 *  @return 转换的文件
 */
fun Uri.toFile(
    pathName: String = "apk",
    extension: String = "",
    newFileName: String? = null
): File? {
    // 文件保存路径
    val cachePath = LanzouApplication.context.externalCacheDir!!.path + "/" + pathName
    val cacheDir = File(cachePath)
    if (!cacheDir.exists()) {
        cacheDir.mkdir()
    }
    if (scheme == ContentResolver.SCHEME_FILE) {
        val fileName = newFileName ?: lastPathSegment
        val outFile = File("$cachePath/$fileName$extension")
        // if (outFile.exists() && outFile.canExecute()) return outFile
        val parcelFileDescriptor =
            LanzouApplication.context.contentResolver.openFileDescriptor(this, "r")
        val fileDescriptor = parcelFileDescriptor?.fileDescriptor ?: return null
        val inputStream = FileInputStream(fileDescriptor)
        val outputStream = FileOutputStream(outFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.flush()
        outputStream.close()
        parcelFileDescriptor.close()
        return outFile
    }
    // 通过 Uri 得到文件名
    // if (!DocumentFile.isDocumentUri(context, this)) return null
    val fileName = newFileName
        ?: (DocumentFile.fromSingleUri(LanzouApplication.context, this)?.name ?: getFileName())
    // 文件保存目录
    val filePath = "$cachePath/$fileName$extension"
    // 输出文件
    val outFile = File(filePath)
    // 如果输出文件存在，则直接进行返回
    // if (outFile.exists()) return outFile
    // 通过 Uri 打开输入流
    /*val i =
        LanzouApplication.context.contentResolver.openAssetFileDescriptor(this, "rwd") ?: return null
    val inputStream = i.createInputStream()*/
    val inputStream = LanzouApplication.context.contentResolver.openInputStream(this)
    // 文件输出流
    val outputStream = FileOutputStream(filePath)
    // 通过流复制文件到缓存目录
    inputStream?.copyTo(outputStream)
    // 输出流关闭
    inputStream?.close()
    outputStream.close()
    // i.close()
    return outFile
}


private fun Uri.getFilePath(): String? {
    LanzouApplication.context.contentResolver.query(this, null, null, null, null)?.apply {
        if (moveToFirst()) {
            return getString(getColumnIndexOrThrow("_data"))
        }
        close()
    }
    return null
}

fun Array<File>.sortFile() = sortedWith { o1, o2 ->
    if (o1.isDirectory && o2.isFile) {
        -1
    } else if (o1.isFile && o2.isDirectory) {
        1
    } else {
        o1.name.lowercase().compareTo(o2.name.lowercase())
    }
}

fun List<FileInfo>.sortFile() = sortedWith { o1, o2 ->
    if (o1.extension == null && o2.extension != null) {
        -1
    } else if (o1.extension != null && o2.extension == null) {
        1
    } else {
        o1.name.lowercase().compareTo(o2.name.lowercase())
    }
}

private const val BASE_URI = "content://com.android.externalstorage.documents/tree/primary%3A"

fun String.path2Uri() = path2UriString().toUri()

fun String.path2UriString(): String {
    var path = this
    if (path.endsWith("/")) {
        path = path.substring(0, path.length - 1)
    }
    val basePath = path.replace("/storage/emulated/0/", "")
        .replace("/", "%2F")

    val index = basePath.indexOf("%2F", 17)
    val primary = if (index == -1) {
        basePath
    } else basePath.substring(0, index)
    return "$BASE_URI$primary/document/primary%3A$basePath"
}

fun Uri.uri2Path(): String {
    return toString().uriString2Path()
}

fun String.uriString2Path(): String {
    val last = lastIndexOf("%3A") + 3
    return "/storage/emulated/0/" + substring(last).replace("%2F", "/")
}

