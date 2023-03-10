package cc.drny.lanzou.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.view.MenuProvider
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import cc.drny.lanzou.R
import cc.drny.lanzou.adapter.BaseAdapter
import cc.drny.lanzou.base.BaseFragment
import cc.drny.lanzou.databinding.ContentToolbarTransmissionBinding
import cc.drny.lanzou.databinding.FragmentTransmissionBinding
import cc.drny.lanzou.event.FileFilterable
import cc.drny.lanzou.ui.download.DownloadFragment
import cc.drny.lanzou.ui.upload.UploadFragment
import cc.drny.lanzou.util.enableMenuIcon
import cc.drny.lanzou.util.showToast
import com.google.android.material.button.MaterialButton

class TransmissionFragment : BaseFragment(), MenuProvider {

    private var _binding: FragmentTransmissionBinding? = null
    private val binding get() = _binding!!

    private var _toolBarBinding: ContentToolbarTransmissionBinding? = null
    private val toolBarBinding get() = _toolBarBinding!!

    private val fragments = mutableListOf<Fragment>()

    var searchKey: String = ""

    private var searchView: SearchView? = null

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
        addMenuProvider(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.let {
            searchKey = it.getString(BaseAdapter.SEARCH_KEY, "")
        }

        binding.viewpager2.adapter = MyPagerAdapter()

        binding.viewpager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                (toolBarBinding.root[position] as MaterialButton).isChecked = true
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(BaseAdapter.SEARCH_KEY, searchKey)
        // 这里取消调用防止离开软件，过滤失效的问题
        searchView?.setOnQueryTextListener(null)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.enableMenuIcon()
        menuInflater.inflate(R.menu.menu_tranmission, menu)
        val searchItem = menu.findItem(R.id.search_file)
        searchView = searchItem.actionView as SearchView
        if (searchKey.isNotEmpty()) {
            searchItem.expandActionView()
            searchView?.setQuery(searchKey, false)
        }

        searchView?.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                searchKey = newText.toString()
                val fragment = fragments[binding.viewpager2.currentItem]
                if (fragment is FileFilterable) {
                    fragment.onFilter(searchKey)
                }
                return true
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
        })
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId != R.id.search_file) {
            "还没做".showToast()
        }
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
        searchView = null
        _binding = null
    }

}