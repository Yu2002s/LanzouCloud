package cc.drny.lanzou.network

import cc.drny.lanzou.event.UploadProgressListener
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Okio
import okio.buffer

class FileRequestBody(
    private val requestBody: RequestBody,
    private val uploadProgressListener: UploadProgressListener
) : RequestBody() {

    private var currentLength = 0L

    override fun contentType() = requestBody.contentType()

    override fun contentLength() = requestBody.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val contentLength = contentLength()
        val forwardingSink = object : ForwardingSink(sink) {
            override fun write(source: Buffer, byteCount: Long) {
                currentLength += byteCount
                uploadProgressListener.onProgress(currentLength, contentLength)
                super.write(source, byteCount)
            }
        }

        val bufferedSink = forwardingSink.buffer()
        requestBody.writeTo(bufferedSink)
        bufferedSink.flush()
    }

}