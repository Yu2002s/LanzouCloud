package cc.drny.lanzou.network

import okhttp3.Headers

class HttpParam {

    companion object {

        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.162 Mobile Safari/537.36 Edg/108.0.0.0"
        const val ACCEPT =
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
        const val ACCEPT_LANGUAGE = "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6"

        val httpHeaders =
            Headers.headersOf(
                "Accept",
                ACCEPT,
                "Accept-Language",
                ACCEPT_LANGUAGE,
                "User-Agent",
                USER_AGENT,
                "Connection",
                "keep-alive"
            )

    }

}