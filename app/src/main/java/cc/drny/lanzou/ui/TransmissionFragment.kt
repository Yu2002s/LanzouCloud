package cc.drny.lanzou.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.view.MenuProvider
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import cc.drny.lanzou.R
import cc.drny.lanzou.base.BaseFragment
import cc.drny.lanzou.base.BaseSuperFragment
import cc.drny.lanzou.databinding.ContentToolbarTransmissionBinding
import cc.drny.lanzou.databinding.FragmentTransmissionBinding
import cc.drny.lanzou.event.FileFilterable
import cc.drny.lanzou.event.Scrollable
import cc.drny.lanzou.ui.download.DownloadFragment
import cc.drny.lanzou.ui.download.DownloadFragmentDirections
import cc.drny.lanzou.ui.upload.UploadFragment
import cc.drny.lanzou.ui.user.LoginFragmentDirections
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.w3c.dom.Text

class TransmissionFragment : BaseFragment(), MenuProvider {

    private var _binding: FragmentTransmissionBinding? = null
    private val binding get() = _binding!!

    private var _toolBarBinding: ContentToolbarTransmissionBinding? = null
    private val toolBarBinding get() = _toolBarBinding!!

    private val fragments = mutableListOf<Fragment>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        _toolBarBinding = ContentToolbarTransmissionBinding.inflate(layoutInflater)
    }

    override fun onDetach() {
        super.onDetach()
        _toolBarBinding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragments.add(DownloadFragment())
        fragments.add(UploadFragment())

        if (savedInstanceState != null) {
            childFragmentManager.fragments.forEach { old ->
                fragments.forEachIndexed { index, new ->
                    if (new::class.simpleName == old::class.simpleName) {
                        fragments[index] = old
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransmissionBinding.inflate(inflater, container, false)

        toolBarBinding.btnDownload.setOnClickListener {
            binding.viewpager2.currentItem = 0
        }

        toolBarBinding.btnUpload.setOnClickListener {
            binding.viewpager2.currentItem = 1
        }

        requireActivity().addMenuProvider(this, viewLifecycleOwner)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewpager2.adapter = MyPagerAdapter()

        binding.viewpager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                (toolBarBinding.root[position] as MaterialButton).isChecked = true
            }
        })
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_tranmission, menu)
        val searchView = menu.findItem(R.id.search_file).actionView as SearchView
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!hasFocus && searchView.query.isNotEmpty()) {
                val fragment = fragments[binding.viewpager2.currentItem]
                if (fragment is FileFilterable) {
                    searchView.requestFocus()
                    searchView.onActionViewExpanded()
                    // TODO: 待完成
                    // fragment.onFilter("")
                }
            }
        }
        searchView.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                val fragment = fragments[binding.viewpager2.currentItem]
                if (fragment is FileFilterable) {
                    fragment.onFilter(newText)
                }
                return false
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
        })
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

    override fun getToolBarCustomView(): View {
        return toolBarBinding.root
    }

    private inner class MyPagerAdapter : FragmentStateAdapter(childFragmentManager, lifecycle) {
        override fun createFragment(position: Int): Fragment = fragments[position]
        override fun getItemCount() = fragments.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        requireActivity().removeMenuProvider(this)
    }

}