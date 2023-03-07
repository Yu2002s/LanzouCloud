package cc.drny.lanzou.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile

class RequestAccessAppDataDir: ActivityResultContract<String, Intent?>() {

    override fun createIntent(context: Context, input: String): Intent {
        // 通过包名构建Uri
        val uri = input.path2Uri()
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.flags = (Intent.FLAG_GRANT_READ_URI_PERMISSION
                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
        }
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Intent? {
        return intent
    }
}