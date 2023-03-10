package cc.drny.lanzou.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import cc.drny.lanzou.util.dp2px
import cc.drny.lanzou.util.getNavigationBarHeight

open class BaseSuperFragment: BaseFragment() {

    companion object {
        private val BOTTOM_NAV_HEIGHT = 80.dp2px()
    }

    fun getInsertBottom() = BOTTOM_NAV_HEIGHT + getNavigationBarHeight()

    var fitNavigationBar = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!fitNavigationBar) return
        getRecyclerView(view as ViewGroup)?.let {
            it.clipToPadding = false
            it.updatePadding(bottom = it.paddingBottom + getInsertBottom())
        }
    }

    private fun getRecyclerView(viewGroup: ViewGroup): RecyclerView? {
        viewGroup.forEach {
            if (it is RecyclerView) {
                return it
            } else if (it is ViewGroup) {
                return getRecyclerView(it)
            }
        }
        return null
    }
}