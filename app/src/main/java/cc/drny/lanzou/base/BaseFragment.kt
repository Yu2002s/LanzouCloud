package cc.drny.lanzou.base

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
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

}