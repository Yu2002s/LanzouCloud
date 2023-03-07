package cc.drny.lanzou.service

import cc.drny.lanzou.data.lanzou.LanzouFileResponse
import cc.drny.lanzou.data.user.LoginFromState
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface UserService {

    @GET("mlogin.php")
    fun getPhpSessionId(): Call<ResponseBody>

    @POST("mlogin.php")
    @FormUrlEncoded
    fun loginLanzou(
        @Header("Cookie") cookie: String,
        @Field("task") task: Int,
        @Field("uid") username: String,
        @Field("pwd") password: String
    ): Call<LoginFromState>



}