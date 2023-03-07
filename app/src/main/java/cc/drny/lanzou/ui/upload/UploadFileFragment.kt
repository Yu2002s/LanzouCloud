package cc.drny.lanzou.ui.upload

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import cc.drny.lanzou.R
import cc.drny.lanzou.base.BaseFragment
import cc.drny.lanzou.databinding.ContentToolbarUploadBinding
import cc.drny.lanzou.databinding.FragmentAndroidDataBinding
import cc.drny.lanzou.databinding.FragmentUploadFileBinding
import cc.drny.lanzou.event.FileFilterable
import cc.drny.lanzou.util.dp2px
import cc.drny.lanzou.util.getNavigationBarHeight
import com.google.android.material.button.MaterialButton
import com.google.android.material.radiobutton.MaterialRadioButton
import kotlinx.coroutines.launch

class UploadFileFragment : BaseFragment(), MenuProvider {

    private var _binding: FragmentUploadFileBinding? = null
    private val binding get() = _binding!!

    private var _toolbarBinding: ContentToolbarUploadBinding? = null
    private val toolbarBinding get() = _toolbarBinding!!

    private val fragments = mutableListOf<Fragment>()

    private lateinit var adapter: MyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragments.add(ClassificationFragment())
        fragments.add(FileManagerFragment())
        fragments.add(FileSearchFragment())
        if (savedInstanceState != null) {
            childFragmentManager.fragments.forEach { old ->
                fragments.forEachIndexed { index, new ->
                    if (old::class.simpleName == new::class.simpleName) {
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
        _binding = FragmentUploadFileBinding.inflate(inflater, container, false)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)

        _toolbarBinding = ContentToolbarUploadBinding.inflate(layoutInflater)

        val listener = OnClickListener {
            binding.viewpager2.setCurrentItem(toolbarBinding.root.indexOfChild(it), false)
        }
        toolbarBinding.apply {
            btnClassification.setOnClickListener(listener)
            btnFile.setOnClickListener(listener)
            btnSearch.setOnClickListener(listener)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("jdy", "size: " + fragments.toString())

        adapter = MyAdapter()
        binding.viewpager2.adapter = adapter
        binding.viewpager2.isUserInputEnabled = false

        binding.viewpager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                (toolbarBinding.root[position] as MaterialButton).isChecked = true
            }
        })

        binding.fabUpload.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = 80.dp2px() + getNavigationBarHeight()
        }

        binding.fabUpload.setOnClickListener {
            findNavController().navigate(UploadFileFragmentDirections
                .actionUploadFileFragmentToUploadFileDialogFragment())
        }
    }

    override fun onCreateMenu(p0: Menu, p1: MenuInflater) {
        p1.inflate(R.menu.menu_upload_file, p0)
        val searchView = p0.findItem(R.id.search_file).actionView as SearchView
        searchView.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                val target = fragments[binding.viewpager2.currentItem]
                if (target is FileFilterable) {
                    target.onFilter(newText)
                }
                return false
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
        })
    }

    override fun onMenuItemSelected(p0: MenuItem): Boolean {
       return  when (p0.itemId) {
            R.id.search_file -> {
                true
            }
           R.id.upload_file -> {
               findNavController().navigate(
                   UploadFileFragmentDirections.actionUploadFileFragmentToUploadFileDialogFragment()
               )
               true
           }
           else -> false
        }
    }

    override fun getToolBarCustomView(): View? {
        return _toolbarBinding?.root
    }

    private inner class MyAdapter :
        FragmentStateAdapter(childFragmentManager, lifecycle) {
        override fun getItemCount() = fragments.size

        override fun createFragment(position: Int) = fragments[position]

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _toolbarBinding = null
        requireActivity().removeMenuProvider(this)
    }

}