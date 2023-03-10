package cc.drny.lanzou.util

import android.content.Intent
import cc.drny.lanzou.LanzouApplication
import java.net.URLEncoder

object ALiPayUtils {

    /**
     *  支付宝包名
     */
    private const val ALIPAY_PACKAGE_NAME = "com.eg.android.AlipayGphone"

    /**
     *  阿里支付代码
     */
    private const val ALI_URL_CODE = "fkx12006cdgchxzqxr1ku5c"

    /**
     *  检查支付宝有没有安装
     */
    fun hasInstalledAlipayClient(): Boolean {
        return LanzouApplication.context.packageManager.getPackageInfo(ALIPAY_PACKAGE_NAME, 0) != null
    }

    /**
     *  启动支付宝支付页面
     */
    fun startAlipayClient() {
        startIntentUri(doFromUri())
    }

    private fun doFromUri(): String {
        var fromUriCode = ""
        try {
            fromUriCode = URLEncoder.encode(ALI_URL_CODE, "utf-8")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "intent://platformapi/startapp?saId=10000007&" +
                "clientVersion=3.7.0.0718&qrcode=https%3A%2F%2Fqr.alipay.com%2F$fromUriCode%3F_s" +
                "%3Dweb-other&_t=1472443966571#Intent;" +
                "scheme=alipayqr;package=com.eg.android.AlipayGphone;end"
    }

    private fun startIntentUri(intentUri: String) {
        try {
            val intent = Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            LanzouApplication.context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
           "未安装支付宝".showToast()
        }
    }

}