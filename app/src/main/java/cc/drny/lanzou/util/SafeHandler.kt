package cc.drny.lanzou.util

import android.app.Activity
import android.app.Service
import android.content.Context
import java.lang.ref.WeakReference

open class SafeHandler<T>(t: T) {

    private val weakReference = WeakReference(t)

    private fun isActivityDestroyed(): Boolean {
        val t = weakReference.get() ?: return true
        if (t is Activity) {
            if (t.isFinishing || t.isDestroyed) {
                return true
            }
        }

        return false
    }

}