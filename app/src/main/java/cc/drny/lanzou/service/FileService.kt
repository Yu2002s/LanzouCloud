package cc.drny.lanzou.service

import cc.drny.lanzou.data.lanzou.*
import cc.drny.lanzou.network.HttpParam
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface FileService {

    /**
     * task 47加载文件夹，5 加载文件
     */
    @POST("doupload.php")
    @FormUrlEncoded
    fun getFiles(
        @Query("uid") uid: Int,
        @Field("task") task: Int,
        @Field("folder_id") folderId: Long,
        @Field("pg") page: Int
    ): Call<LanzouFileResponse>

    /**
     * task 2 新建文件夹
     */
    @POST("doupload.php")
    @FormUrlEncoded
    fun createFolder(
        @Field("task") task: Int,
        @Field("parent_id") parentId: Long,
        @Field("folder_name") folderName: String,
        @Field("folder_description") folderDesc: String
    ): Call<LanzouSimpleResponse>

    /**
     * 上传文件
     */
    @POST("fileup.php")
    fun uploadFile(@Body requestBody: RequestBody): Call<LanzouUploadResponse>

    /**
     * task 19 获取所有文件夹
     */
    @POST("doupload.php")
    @FormUrlEncoded
    fun getAllFolder(@Field("task") task: Int): Call<LanzouFolderResponse>

    /**
     * task 22 获取文件的分享链
     */
    @POST("doupload.php")
    @FormUrlEncoded
    fun getShareUrl(
        @Field("task") task: Int,
        @Field("file_id") fileId: Long
    ): Call<LanzouUrlResponse>

    /**
     *  有密码的文件获取下载地址
     */
    @POST
    @FormUrlEncoded
    @Headers("User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1 Edg/106.0.0.0")
    fun getDownloadUrl(
        @Url url: String,
        @Header("Referer") referer: String,
        @Field("action") action: String,
        @Field("sign") sign: String,
        @Field("p") password: String
    ): Call<LanzouDownloadUrlResponse>

    /**
     *  task 6 删除文件 3 删除文件夹
     */
    @POST("doupload.php")
    @FormUrlEncoded
    fun deleteFileOrFolder(
        @FieldMap map: Map<String, String>
    ): Call<LanzouSimpleResponse>

    /**
     *  task 2 新建文件夹
     */
    @POST("doupload.php")
    @FormUrlEncoded
    fun newFolder(
        @Field("task") task: Int,
        @Field("parent_id") parentId: Long,
        @Field("folder_name") folderName: String,
        @Field("folder_description") folderDesc: String
    ): Call<LanzouSimpleResponse>

    /**
     *  task 18 得到文件夹信息
     */
    @POST("doupload.php")
    @FormUrlEncoded
    fun getFolder(
        @Field("task") task: Int,
        @Field("folder_id") folderId: Long
    ): Call<LanzouUrlResponse>

    /**
     *  task 20 移动文件
     */
    @POST("doupload.php")
    @FormUrlEncoded
    fun moveFile(
        @Field("task") task: Int,
        @Field("file_id") fileId: Long,
        @Field("folder_id") folderId: Long
    ): Call<LanzouSimpleResponse>

    /**
     *  task 23 修改文件密码
     */
    @POST("doupload.php")
    @FormUrlEncoded
    fun editFilePassword(
        @Field("task") task: Int,
        @Field("file_id") fileId: Long,
        @Field("shows") enablePwd: Int,
        @Field("shownames") password: String
    ): Call<LanzouSimpleResponse>

    /**
     *  task 16 修改文件夹密码
     */
    @POST("doupload.php")
    @FormUrlEncoded
    fun editFolderPassword(
        @Field("task") task: Int,
        @Field("folder_id") fileId: Long,
        @Field("shows") enablePwd: Int,
        @Field("shownames") password: String
    ): Call<LanzouSimpleResponse>

    /**
     *  task 12 文件描述信息
     */
    @POST("doupload.php")
    @FormUrlEncoded
    fun getFileDescribe(
        @Field("task") task: Int,
        @Field("file_id") fileId: Long
    ): Call<LanzouSimpleResponse>

    /**
     *  task 11 保存文件描述信息
     */
    @POST("doupload.php")
    @FormUrlEncoded
    fun saveFileDescribe(
        @Field("task") task: Int,
        @Field("file_id") fileId: Long,
        @Field("desc") describe: String
    ): Call<LanzouSimpleResponse>

    /**
     *  task 18 得到文件夹的描述信息
     */
    @POST("doupload.php")
    @FormUrlEncoded
    fun getFolderDescribe(
        @Field("task") task: Int,
        @Field("folder_id") folderId: Long
    ): Call<LanzouUrlResponse>

    /**
     *  task 4 保存文件夹的描述信息
     */
    @POST("doupload.php")
    @FormUrlEncoded
    fun saveFolderDescribe(
        @Field("task") task: Int,
        @Field("folder_id") folderId: Long,
        @Field("folder_name") folderName: String,
        @Field("folder_description") describe: String
    ): Call<LanzouSimpleResponse>

    /**
     * 恢复或删除回收站中的文件
     */
    @POST
    @FormUrlEncoded
    fun restoreOrDeleteFile(
        @Url url: String,
        @FieldMap fieldMap: Map<String, String>
    ): Call<ResponseBody>

    /**
     * 删除或恢复回收站中的所有文件
     */
    @POST
    @FormUrlEncoded
    fun deleteOrRestoreRecycleBin(
        @Url url: String,
        @Field("action") action: String,
        @Field("task") task: String,
        @Field("formhash") hash: String
    ): Call<ResponseBody>

    /**
     * 获取分享文件夹中的文件
     */
    @POST
    @FormUrlEncoded
    @Headers(
        "User-Agent: Mozilla/5.0 (Linux; Android 13; 22041211AC Build/TP1A.220624.014) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/104.0.5112.97 Mobile Safari/537.36",
        "Accept: application/json, text/javascript, */*",
        "Accept-Language: zh-CN,zh;q\\u003d0.9,en-US;q\\u003d0.8,en;q\\u003d0.7",
    )
    fun getLanzouFilesForUrl(
        @Url url: String,
        @FieldMap map: Map<String, String>
    ): Call<LanzouAnalyzingFileResponse>
}