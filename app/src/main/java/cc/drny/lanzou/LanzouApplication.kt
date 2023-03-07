package cc.drny.lanzou

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.tencent.mmkv.MMKV
import com.tencent.mmkv.MMKVLogLevel
import org.litepal.LitePal

class LanzouApplication: Application() {

    companion object {
        const val LANZOU_HOST = "https://pc.woozooo.com/"
        const val LANZOU_HOST_FILE = "${LANZOU_HOST}mydisk.php"
        const val LANZOU_HOST_REGISTER = "${LANZOU_HOST}account.php?action=register"
        const val LANZOU_HOST_MYSELF = "${LANZOU_HOST}myfile.php?item=1&v2"
        const val LANZOU_HOST_FORGET = "${LANZOU_HOST}account.php?action=forget_pwd"
        const val LANZOU_HOST_LOGIN = "${LANZOU_HOST}account.php?action=login"
        const val LANZOU_HOST_RECYCLE = "$LANZOU_HOST_FILE?item=recycle&action=files"
        const val LANZOU_HOST_RECYCLE_ACTION = "${LANZOU_HOST_FILE}?item=recycle"

        /**
         * 文件下载地址域名
         */
        const val LANZOU_HOST_DOWNLOAD = "https://developer.lanzoug.com/file/"

        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = this
        // 初始化MMKV
        MMKV.initialize(this, MMKVLogLevel.LevelNone)
        LitePal.initialize(this)
    }
}