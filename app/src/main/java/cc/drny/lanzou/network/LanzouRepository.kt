package cc.drny.lanzou.network

import android.content.Context
import android.text.style.IconMarginSpan
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.lifecycle.liveData
import cc.drny.lanzou.LanzouApplication
import cc.drny.lanzou.R
import cc.drny.lanzou.data.lanzou.*
import cc.drny.lanzou.data.update.UpdateResponse
import cc.drny.lanzou.data.user.User
import cc.drny.lanzou.event.UploadProgressListener
import cc.drny.lanzou.service.FileService
import cc.drny.lanzou.service.UserService
import cc.drny.lanzou.util.LanzouAnalyzeException
import cc.drny.lanzou.util.getAppIcon
import cc.drny.lanzou.util.getIcon
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.await
import retrofit2.awaitResponse
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import java.io.File

object LanzouRepository {

    /**
     *  登录
     */
    private const val TASK_LOGIN = 3

    /**
     * 获取文件
     */
    private const val TASK_GET_FILES = 5

    /**
     *  获取文件夹
     */
    private const val TASK_GET_FOLDERS = 47

    /**
     * 创建文件夹
     */
    private const val TASK_CREATE_FOLDER = 2

    /**
     * 上传文件
     */
    private const val TASK_UPLOAD_FILE = 1

    /**
     * 获取所有的文件夹
     */
    private const val TASK_ALL_FOLDER = 19

    /**
     * 获取文件分享链
     */
    private const val TASK_GET_URL = 22

    /**
     * 删除文件
     */
    private const val TASK_DELETE_FILE = 6

    /**
     * 删除文件夹
     */
    private const val TASK_DELETE_FOLDER = 3

    /**
     * 新建文件夹
     */
    private const val TASK_NEW_FOLDER = 2

    /**
     * 获取文件夹信息
     */
    private const val TASK_GET_FOLDER = 18

    /**
     * 移动文件
     */
    private const val TASK_MOVE_FILE = 20

    /**
     * 修改密码
     */
    private const val TASK_EDIT_FILE_PASSWORD = 23

    /**
     * 修改文件夹密码
     */
    private const val TASK_EDIT_FOLDER_PASSWORD = 16

    /**
     * 文件描述
     */
    private const val TASK_FILE_DESCRIBE = 12

    /**
     * 保存文件描述信息
     */
    private const val TASK_SAVE_FILE_DESCRIBE = 11

    /**
     * 保存文件夹描述信息
     */
    private const val TASK_SAVE_FOLDER_DESCRIBE = 4

    /**
     * 允许上传的所有类型
     */
    private val allowUploadTypes by lazy {
        arrayOf(
            "doc", "docx", "zip", "rar", "apk", "ipa", "txt", "exe",
            "7z", "e", "z", "ct", "ke", "db", "tar", "pdf",
            "w3xepub", "mobi", "azw", "azw3", "osk", "osz", "xpa", "cpk",
            "lua", "jar", "dmg", "ppt", "pptx", "xls", "xlsx", "mp3", "ipa",
            "iso", "img", "gho", "ttf", "ttc", "txf", "dwg", "bat",
            "dll", "crx", "xapk", "rp", "rpm", "rplib",
            "appimage", "lolgezi", "flac", "cad", "hwt", "accdb", "ce", "xmind", "enc",
            "bds", "bdi", "ssd", "it"
        )
    }

    /**
     * 用于获取文件真实后缀的正则表达式
     */
    private val FILE_REGEX = "(.+)\\.([a-zA-Z]+\\d?)\\.apk".toRegex()

    /**
     * 存取配置信息
     */
    private val mmkv = MMKV.defaultMMKV(MMKV.SINGLE_PROCESS_MODE, "jdy200255")

    /**
     * 用户身份信息标识
     */
    private var cookie: String? = getUserCookie()

    /**
     * 用户唯一ID
     */
    private var UID = 0

    private val okHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .addInterceptor {
            var request = it.request()
            val cookieHeader = request.header("Cookie")
            if (cookieHeader == null) {
                val userCookie = getUserCookie()
                if (userCookie != null) {
                     request = request.newBuilder().addHeader("Cookie", userCookie).build()
                }
            }
            val response = it.proceed(request)
            if (cookie == null) {
                response.header("Set-Cookie")?.let { setCookie ->
                    saveUserCookie(setCookie)
                }
            }
            response
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(LanzouApplication.LANZOU_HOST)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()

    private val userService = retrofit.create<UserService>()
    private val lanzouService = retrofit.create<FileService>()

    /**
     * 检查更新
     */
    suspend fun checkUpdate(versionCode: Long) = catchResult {
        val formBody = FormBody.Builder()
            .add("code", versionCode.toString())
            .build()
        val request = Request.Builder()
            .url("http://180.76.101.239/lanzou/update/update.php")
            .post(formBody)
            .build()
        val response = okHttpClient.newCall(request).execute()
        val body = response.body ?: throw IllegalStateException()
        val msg = body.string()
        val updateResponse = Gson().fromJson(msg, UpdateResponse::class.java)
        if (updateResponse.status == "ok") {
            Result.success(updateResponse.update)
        } else {
            Result.failure(Throwable(updateResponse.msg))
        }
    }

    /**
     *  登录到蓝奏云并保存Cookie到本地
     */
    fun login(user: User) = fire {
        val username = user.username
        val password = user.password
        if (username.isBlank() || password.isBlank()) {
            Result.failure(Throwable("请输入用户名或密码"))
        } else {
            logout()
            val sessionId = userService.getPhpSessionId().awaitResponse()
            // 获取页面的Cookie，此Cookie为自动生成的
            val setCookie = sessionId.raw().header("Set-Cookie")
            if (setCookie == null) {
                Result.failure(Throwable("错误" + sessionId.code()))
            } else {
                val phpSessionId = setCookie.split(";")[0]
                val loginResponse =
                    userService.loginLanzou(phpSessionId, TASK_LOGIN, username, password)
                        .awaitResponse()
                val cookies = loginResponse.headers().values("Set-Cookie")
                if (cookies.isEmpty()) {
                    Result.failure(Throwable("登录失败了"))
                } else {
                    val cookie = with(StringBuilder(phpSessionId)) {
                        cookies.forEach {
                            append(";")
                            append(it)
                        }
                        toString()
                    }
                    // 保存Cookie
                    saveUserCookie(cookie)
                    delay(200)
                    Result.success(cookie)
                }
            }
        }
    }

    suspend fun getFiles(lanzouPage: LanzouPage): Result<List<LanzouFile>> {
        if (cookie == null) return Result.failure(Throwable("请登录你的账号"))
        val lanzouFiles = mutableListOf<LanzouFile>()
        return try {
            if (UID == 0) {
                getUserId()
            }
            if (lanzouPage.page == 1) {
                lanzouFiles.addAll(getFolders(lanzouPage.folderId))
            }
            lanzouFiles.addAll(getFiles(lanzouPage.folderId, lanzouPage.page))
            Result.success(lanzouFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFolder(parentId: Long, folderName: String, folderDesc: String) = catchResult {
        val simpleResponse =
            lanzouService.createFolder(TASK_CREATE_FOLDER, parentId, folderName, folderDesc).await()
        if (simpleResponse.status == 1) {
            Result.success(simpleResponse.text.toLong())
        } else {
            Result.failure(Throwable("创建文件夹失败"))
        }
    }

    private suspend fun getFolders(folderId: Long = -1): List<LanzouFile> {
        return getFiles(TASK_GET_FOLDERS, folderId)
    }

    private suspend fun getFiles(folderId: Long, page: Int): List<LanzouFile> {
        return getFiles(TASK_GET_FILES, folderId, page)
    }

    private suspend fun getFiles(task: Int, folderId: Long, page: Int = 1): List<LanzouFile> {
        val lanzouFileResponse =
            lanzouService.getFiles(UID, task, folderId, page).await()
        return lanzouFileResponse.files
    }

    /**
     * 获取所有的文件夹
     */
    suspend fun getAllFolder() = catchOrNull<List<LanzouFolder>> {
        val lanzouFolderResponse = withContext(Dispatchers.IO) {
            lanzouService.getAllFolder(TASK_ALL_FOLDER).await()
        }
        if (lanzouFolderResponse.status == 1) {
            (lanzouFolderResponse.folders as MutableList).also {
                it.add(0, LanzouFolder(-1, "根目录"))
            }
        } else {
            throw NullPointerException()
        }
    }

    /**
     * 删除文件或文件夹
     */
    suspend fun deleteFileOrFolder(id: Long, isFile: Boolean = true) = catchResult {
        val map = mutableMapOf<String, String>()
        if (isFile) {
            map["task"] = TASK_DELETE_FILE.toString()
            map["file_id"] = id.toString()
        } else {
            map["task"] = TASK_DELETE_FOLDER.toString()
            map["folder_id"] = id.toString()
        }
        val lanzouResponse = lanzouService.deleteFileOrFolder(map).await()
        if (lanzouResponse.status == 1) {
            Result.success(lanzouResponse.info)
        } else {
            Result.failure(Throwable(lanzouResponse.info))
        }
    }

    /**
     * 新建文件夹
     */
    suspend fun newFolder(folderId: Long, name: String, desc: String) = catchResult {
        val simpleResponse = lanzouService.newFolder(TASK_NEW_FOLDER, folderId, name, desc).await()
        if (simpleResponse.status == 1) {
            Result.success(simpleResponse.info)
        } else {
            Result.failure(Throwable(simpleResponse.info))
        }
    }

    suspend fun getFolder(folderId: Long) = catchResult {
        val lanzouUrlResponse = lanzouService.getFolder(TASK_GET_FOLDER, folderId).await()
        if (lanzouUrlResponse.status == 1) {
            Result.success(lanzouUrlResponse.info)
        } else {
            Result.failure(Throwable("获取文件夹信息失败"))
        }
    }

    suspend fun moveFile(fileId: Long, folderId: Long) = catchResult {
        val simpleResponse = lanzouService.moveFile(TASK_MOVE_FILE, fileId, folderId).await()
        if (simpleResponse.status == 1) {
            Result.success(simpleResponse.info)
        } else {
            Result.failure(Throwable(simpleResponse.info))
        }
    }

    suspend fun uploadFile(
        folderId: Long,
        file: File,
        name: String,
        listener: UploadProgressListener
    ): LanzouUploadResponse {
        var fileName = name
        val extension = file.extension
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

        val requestBody = file.asRequestBody(mimeType.toMediaType())

        if (!allowUploadTypes.contains(extension)) {
            fileName += ".apk"
        }

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("task", TASK_UPLOAD_FILE.toString())
            .addFormDataPart("folder_id", folderId.toString())
            .addFormDataPart("upload_file", fileName, requestBody)
            .build()

        val fileRequestBody = FileRequestBody(multipartBody, listener)

        return lanzouService.uploadFile(fileRequestBody).await()
    }

    suspend fun getFileInfo(fileId: Long) = catchResult {
        val lanzouUrlResponse = lanzouService.getShareUrl(TASK_GET_URL, fileId).await()
        if (lanzouUrlResponse.status == 1) {
            Result.success(lanzouUrlResponse.info)
        } else {
            throw IllegalStateException("获取分享链出错")
        }
    }

    suspend fun editFilePassword(fileId: Long, enablePwd: Boolean, password: String) = catchResult {
        val enable = if (enablePwd) 1 else 0
        val simpleResponse =
            lanzouService.editFilePassword(TASK_EDIT_FILE_PASSWORD, fileId, enable, password)
                .await()
        if (simpleResponse.status == 1) {
            Result.success(simpleResponse.info)
        } else {
            Result.failure(Throwable(simpleResponse.info))
        }
    }

    suspend fun editFolderPassword(folderId: Long, enablePwd: Boolean, password: String) =
        catchResult {
            val enable = if (enablePwd) 1 else 0
            val simpleResponse = lanzouService.editFolderPassword(
                TASK_EDIT_FOLDER_PASSWORD,
                folderId,
                enable,
                password
            ).await()
            if (simpleResponse.status == 1) {
                Result.success(simpleResponse.info)
            } else {
                Result.failure(Throwable(simpleResponse.info))
            }
        }

    suspend fun getFileDescribe(fileId: Long) = catchResult {
        val simpleResponse = lanzouService.getFileDescribe(TASK_FILE_DESCRIBE, fileId).await()
        if (simpleResponse.status == 1) {
            Result.success(simpleResponse.info)
        } else {
            Result.failure(Throwable(simpleResponse.info))
        }
    }

    suspend fun saveFileDescribe(fileId: Long, describe: String) = catchResult {
        val simpleResponse =
            lanzouService.saveFileDescribe(TASK_SAVE_FILE_DESCRIBE, fileId, describe).await()
        if (simpleResponse.status == 1) {
            Result.success(simpleResponse.info)
        } else {
            Result.failure(Throwable(simpleResponse.info))
        }
    }

    suspend fun saveFolderDescribe(folderId: Long, name: String, describe: String) = catchResult {
        val simpleResponse =
            lanzouService.saveFolderDescribe(TASK_SAVE_FOLDER_DESCRIBE, folderId, name, describe)
                .await()
        if (simpleResponse.status == 1) {
            Result.success(simpleResponse.info)
        } else {
            Result.failure(Throwable(simpleResponse.info))
        }
    }

    fun getShareUrl(lanzouUrl: LanzouUrl): String {
        return lanzouUrl.host + "/tp/" + lanzouUrl.fid
    }

    /**
     * 通过url获取下载地址
     */
    suspend fun getDownloadUrl(
        url: String,
        pwd: String? = null,
        block: ((LanzouFile) -> Unit)? = null
    ) = catchResult {
        var newUrl = url
        if ("/tp/" !in url) {
            newUrl = url.replace(".com/", ".com/tp/")
        }
        Log.d("jdy", newUrl)
        val document = getHtmlDocument(newUrl, true)
        val html = document.html()
        block?.invoke(getLanzouFileForHtml(document))
        if (!pwd.isNullOrEmpty()) {
            val regex = Regex("var postsign = '(.*?)';")
            val matchResult = regex.find(html)
            if (matchResult != null) {
                val sign = matchResult.destructured.component1()
                val index = url.lastIndexOf("/") + 1
                val host = url.substring(0, index).replace("tp/", "")
                val fid = url.substring(index)
                val referer = "${host}tp/$fid"
                val downloadUrlResponse =
                    lanzouService.getDownloadUrl(
                        "${host}ajaxm.php",
                        referer,
                        "downprocess",
                        sign,
                        pwd
                    ).await()
                val status = downloadUrlResponse.status
                if (status == 1) {
                    val downloadUrl =
                        downloadUrlResponse.dom + "/file/" + downloadUrlResponse.url
                    Result.success(downloadUrl)
                } else throw IllegalStateException("获取文件信息出错")
            } else throw IllegalStateException("解析文件出错")
        } else {
            val regex = Regex("submit.href = (.+) \\+ (.+)")
            val matchResult = regex.find(html)
            if (matchResult != null) {
                val parma = matchResult.destructured.component2()
                val word = matchResult.destructured.component1().trim()
                val regex2 = Regex("var $word = \'(.+)\';")
                val regex3 = Regex("var $parma = \'(.+)\'")
                val matchResult2 = regex2.find(html)
                val matchResult3 = regex3.find(html)
                if (matchResult2 != null && matchResult3 != null) {
                    val fileHost = matchResult2.destructured.component1()
                    val downloadUrl = matchResult3.destructured.component1()
                    Result.success(fileHost + downloadUrl)
                } else {
                    Result.success(LanzouApplication.LANZOU_HOST_DOWNLOAD + parma)
                }
            } else throw IllegalStateException("解析文件出错")
        }
    }

    private fun getLanzouFileForHtml(document: Document): LanzouFile {
        if (document.body().text().isEmpty()) {
            // 这个是一个文件夹
            throw LanzouAnalyzeException()
        }
        val element = document.selectFirst("div.mb") ?: throw IllegalStateException("资源解析失败")
        val md = element.selectFirst("div.md") ?: throw IllegalStateException("资源解析失败")
        val name = md.ownText()
        val size = md.selectFirst("span")?.text()
            ?.replace("( ", "")
            ?.replace(" )", "") ?: ""
        val mf = element.selectFirst("div.mf") ?: throw IllegalStateException("资源解析失败")
        val href = mf.select("a").attr("href")
        val id = href.substring(href.indexOf("f=") + 2, href.indexOf("&")).toLong()
        val time = mf.ownText()
        val lanzouFile = LanzouFile()
        if (name.endsWith(".apk")) {
            val matchResult = FILE_REGEX.find(name)
            if (matchResult != null) {
                lanzouFile.icon = matchResult.destructured.component2()
                lanzouFile.name_all =
                    matchResult.destructured.component1() + "." + lanzouFile.icon
            } else {
                lanzouFile.name_all = name
                lanzouFile.icon = "apk"
            }
        } else {
            val index = name.lastIndexOf(".") + 1
            lanzouFile.name_all = name
            lanzouFile.icon = name.substring(index)
        }
        lanzouFile.fileId = id
        lanzouFile.size = size
        lanzouFile.time = time
        return lanzouFile
    }

    /**
     *  显示文件夹内所有文件
     *  https://pc.woozooo.com/mydisk.php?item=recycle&action=show_files&folder_id=5147879
     *  恢复文件提示
     *  https://pc.woozooo.com/mydisk.php?item=recycle&action=file_restore&file_id=80016198
     *  开始恢复
     *  mydisk.php?item=recycle
     */
    fun getRecycleBinFile(context: Context) = fire {
        val document = getHtmlDocument(LanzouApplication.LANZOU_HOST_RECYCLE)
        val elements = document.getElementsByClass("my")
        val folderIcon = ContextCompat.getDrawable(context, R.drawable.baseline_folder_24)
        val regex = Regex("(.+)\\.([a-zA-Z]+\\d?)\\.apk")
        val fileList = mutableListOf<LanzouFile>()
        elements.forEach { element ->
            val a = element.selectFirst("a")!!
            val aHref = a.attr("href")
            val name = a.text()
            val nextElementSibling = element.nextElementSibling()!!
            val spans = nextElementSibling.select("span")
            val size = spans[0].text()
            val time = spans[1].text()
            val href = spans[2].selectFirst("a")!!.attr("href")
            val isFile = aHref.startsWith("http")
            val lanzouFile = LanzouFile()
            lanzouFile.time = time
            lanzouFile.size = size
            val index: Int
            if (isFile) {
                lanzouFile.name_all = name
                index = href.lastIndexOf("file_id=") + 8
                val id = href.substring(index).toLong()
                lanzouFile.fileId = id
                val extension = name.substring(name.lastIndexOf(".") + 1)
                lanzouFile.icon = extension
                if (extension == "apk") {
                    val matchResult = regex.find(name)
                    if (matchResult != null) {
                        lanzouFile.icon = matchResult.destructured.component2()
                        lanzouFile.name_all =
                            matchResult.destructured.component1() + "." + lanzouFile.icon
                    }
                }
                lanzouFile.iconDrawable = lanzouFile.icon.getIcon(context)
            } else {
                lanzouFile.name = name
                index = href.lastIndexOf("folder_id=") + 10
                val id = href.substring(index).toLong()
                lanzouFile.folderId = id
                lanzouFile.iconDrawable = folderIcon
            }
            fileList.add(lanzouFile)
        }
        Result.success(fileList as List<LanzouFile>)
    }

    suspend fun restoreOrDeleteFile(
        fileId: Long,
        isRestore: Boolean = true,
        isFolder: Boolean = false
    ): Result<String> {
        return try {
            val key = if (isFolder) "folder" else "file"
            val action = if (isRestore) {
                if (!isFolder) {
                    "file_restore"
                } else {
                    "folder_restore"
                }
            } else {
                if (!isFolder) {
                    "file_delete_complete"
                } else {
                    "folder_delete_complete"
                }
            }
            val document =
                getHtmlDocument(LanzouApplication.LANZOU_HOST_FILE + "?item=recycle&action=$action&${key}_id=$fileId")
            val s = document.selectFirst("form")!!.select("input")[4].`val`()
            val map = mapOf(
                "action" to action,
                "task" to action,
                "${key}_id" to fileId.toString(),
                "ref" to LanzouApplication.LANZOU_HOST_RECYCLE,
                "formhash" to s
            )
            val html2 =
                lanzouService.restoreOrDeleteFile(
                    LanzouApplication.LANZOU_HOST_FILE + "?item=recycle",
                    map
                )
                    .await().string()
            val document2 = Jsoup.parse(html2)
            val text = document2.select("div.tb_box_msg").text()
            return Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除或恢复回收站中的所有文件
     * https://up.woozooo.com/mydisk.php?item=recycle&action=delete_all
     */
    suspend fun restoreOrDeleteRecycleBin(isRestore: Boolean): Result<String> {
        val action = if (isRestore) "restore_all" else "delete_all"
        val url = LanzouApplication.LANZOU_HOST_RECYCLE_ACTION + "&action=" + action
        return try {
            val html = getHtmlString(url)
            val regex = Regex("name=\"formhash\" value=\"(\\w+)\"")
            val matchResult = regex.find(html)
            if (matchResult != null) {
                // 获取提交时需要的hash值
                val formHash = matchResult.destructured.component1()
                val responseBody = lanzouService.deleteOrRestoreRecycleBin(
                    LanzouApplication.LANZOU_HOST_RECYCLE_ACTION,
                    action,
                    action,
                    formHash
                ).await()
                val reader = responseBody.charStream()
                val body = reader.use {
                    reader.readText()
                }
                val document = Jsoup.parse(body)
                val result = document.getElementsByClass("info_b2").text()
                return Result.success(result)
            }
            Result.failure(Throwable("操作出错了"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLanzouFilesForUrl(url: String, pwd: String? = null, page: Int = 1) = catchResult {
        val document = getHtmlDocument(url, true)
        val html = document.html()
        val map = mutableMapOf<String, String>()
        val regex = Regex("'fid':(\\d+),[\\s\\n]+'uid':'(\\d+)',")
        val matchResult = regex.find(html) ?: throw IllegalStateException("11111111")
        val regex2 = Regex("'t':(.+),[\\s\\n]+'k':(.+),")
        val matchResult2 = regex2.find(html) ?: throw IllegalStateException("222222")
        val fid = matchResult.destructured.component1()
        val uid = matchResult.destructured.component2()
        val t = matchResult2.destructured.component1()
        val k = matchResult2.destructured.component2()
        val regex3 = Regex("var $t = '(\\d+)';\\s+var $k = '([\\da-z]+)';")
        val matchResult3 = regex3.find(html) ?: throw IllegalStateException("获取资源失败222")
        map["lx"] = "2"
        map["fid"] = fid
        map["uid"] = uid
        map["rep"] = "0"
        map["t"] = matchResult3.destructured.component1()
        map["k"] = matchResult3.destructured.component2()
        map["up"] = "1"
        map["ls"] = "1"
        map["pg"] = page.toString()
        if (pwd != null) {
            map["pwd"] = pwd
        }
        val index = url.lastIndexOf("/") + 1
        val host = url.substring(0, index) + "filemoreajax.php"
        val lanzouAnalyzingFileResponse = lanzouService.getLanzouFilesForUrl(host, map).await()
        if (lanzouAnalyzingFileResponse.status == 1) {
            val files = lanzouAnalyzingFileResponse.files
                .map {
                    if (it.extension == "apk") {
                        val nameResult = FILE_REGEX.find(it.name_all)
                        if (nameResult != null) {
                            it.extension = nameResult.destructured.component2()
                            it.name_all = nameResult.destructured.component1() +
                                    "." + it.extension
                        }
                    }
                    LanzouFile(
                        name_all = it.name_all,
                        fid = it.id,
                        size = it.size,
                        time = it.time,
                        icon = it.extension
                    )
                }
            Result.success(files)
        } else {
            Result.failure(Throwable(lanzouAnalyzingFileResponse.info))
        }
    }

    private fun getAnalyzeFileParams() {

    }

    /**
     * 获取文件真实后缀
     */
    fun getFileRealExtension(lanzouFile: LanzouFile) {
        if (lanzouFile.icon == "apk") {
            val matchResult = FILE_REGEX.find(lanzouFile.name_all)
            if (matchResult != null) {
                lanzouFile.icon = matchResult.destructured.component2()
                lanzouFile.name_all =
                    matchResult.destructured.component1() + "." + lanzouFile.icon
            }
        }
    }

    /**
     * 获取用户ID
     */
    private fun getUserId(): Int {
        if (UID != 0) return UID
        UID = mmkv.decodeInt("uid", 0)
        if (UID != 0) return UID
        val document = getHtmlDocument(LanzouApplication.LANZOU_HOST_FILE)
        val src = document.getElementById("mainframe")?.attr("src")
        if (src != null) {
            val index = src.indexOf("u=") + 2
            UID = src.substring(index).toInt()
            mmkv.encode("uid", UID)
        }
        return UID
    }

    private fun getHtmlDocument(url: String, ignoreCookie: Boolean = false): Document {
        val connection = Jsoup.connect(url)
            .header("User-Agent", HttpParam.USER_AGENT)
            .header("Accept", HttpParam.ACCEPT)
            .header("Accept-Language", HttpParam.ACCEPT_LANGUAGE)
        if (!ignoreCookie && cookie != null) {
            connection.cookie("Cookie", cookie!!)
        }
        return connection.get()
    }

    private fun getHtmlString(url: String, ignoreCookie: Boolean = false): String {
        return getHtmlDocument(url, ignoreCookie).html()
    }

    private suspend fun <T> catchOrNull(block: suspend () -> T): T? {
        return try {
            block.invoke()
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun <T> catchResult(block: suspend () -> Result<T>): Result<T> {
        return try {
            block.invoke()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun <T> fire(block: suspend () -> Result<T>) = liveData(Dispatchers.IO) {
        val result = try {
            block.invoke()
        } catch (e: Exception) {
            Result.failure(e)
        }
        emit(result)
    }

    fun isLogin() = cookie != null

    fun logout() {
        cookie = null
        UID = 0
        mmkv.remove("uid")
        mmkv.remove("Cookie")
    }

    fun getUserCookie(): String? {
        if (cookie != null) return cookie
        return mmkv.decodeString("Cookie", null)
    }

    fun saveUserCookie(cookie: String) {
        this.cookie = cookie
        mmkv.encode("Cookie", cookie)
    }

}