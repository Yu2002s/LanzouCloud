package cc.drny.lanzou.base

import android.view.View
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import cc.drny.lanzou.MainActivity

open class BaseFragment: Fragment() {

    val mainActivity get() = requireActivity() as MainActivity

    val toolBar get() = mainActivity.supportActionBar

    open fun getAppBarCustomView(): View? {
        return null
    }

    open fun getToolBarCustomView(): View? {
        return null
    }

    fun addMenuProvider(menuProvider: MenuProvider) {
        viewLifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        requireActivity().removeMenuProvider(menuProvider)
                        requireActivity().addMenuProvider(menuProvider)
                    }
                    Lifecycle.Event.ON_STOP -> {
                        requireActivity().removeMenuProvider(menuProvider)
                    }
                    else -> {}
                }
            }
        })
    }

}