package cc.drny.lanzou.ui.upload

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.core.view.isInvisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.viewbinding.ViewBinding
import cc.drny.lanzou.R
import cc.drny.lanzou.adapter.FileSelectorAdapter
import cc.drny.lanzou.base.BaseSuperFragment
import cc.drny.lanzou.data.upload.FileInfo
import cc.drny.lanzou.databinding.FragmentLocalFileBinding
import cc.drny.lanzou.databinding.ItemListFileSelectorBinding
import cc.drny.lanzou.event.FileFilterable
import cc.drny.lanzou.event.OnItemClickListener
import cc.drny.lanzou.event.Scrollable
import cc.drny.lanzou.ui.file.FileFragmentDirections
import com.google.android.material.card.MaterialCardView

class LocalFileFragment : BaseSuperFragment(), MenuProvider, FileFilterable, Scrollable {

    private var _binding: FragmentLocalFileBinding? = null
    private val binding get() = _binding!!

    private val files = mutableListOf<FileInfo>()
    private val fileSelectorAdapter = FileSelectorAdapter(files)

    private lateinit var navController: NavController

    private val args by navArgs<LocalFileFragmentArgs>()

    private val viewModel by viewModels<LocalFileViewModel> {
        LocalFileViewModel.LocalFileViewModelFactory(fileViewModel.selectedList)
    }
    private val fileViewModel by navGraphViewModels<UploadSelectorViewModel>(R.id.uploadFileFragment)

    private var isShowFab = false

    override fun onCreateMenu(p0: Menu, p1: MenuInflater) {
        p1.inflate(R.menu.menu_upload_file, p0)
        val searchView = p0.findItem(R.id.search_file).actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                onFilter(newText)
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
                    FileFragmentDirections.actionGlobalUploadFileDialogFragment()
                )
                true
            }
            else -> false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navController = findNavController()
        isShowFab = navController.currentDestination!!.id == R.id.uploadFileFragment
        fileSelectorAdapter.onItemClickListener =
            object : OnItemClickListener<FileInfo, ViewBinding> {
                override fun onItemClick(data: FileInfo, v: View) {
                    if (data.extension == null) {
                        findNavController().navigate(
                            LocalFileFragmentDirections
                                .actionGlobalLocalFileFragment(data.path, data.name)
                        )
                        return
                    }
                    data.isSelected = !data.isSelected
                    (v as MaterialCardView).isChecked = data.isSelected
                    if (data.isSelected) {
                        // add
                        Log.d("jdy", "add")
                        fileViewModel.addSelect(data)
                    } else {
                        // remove
                        Log.d("jdy", "remove")
                        fileViewModel.removeSelect(data)
                    }
                }
            }
        fileSelectorAdapter.enableAutoLoad().scopeIn(lifecycleScope)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocalFileBinding.inflate(inflater, container, false)
        if (!isShowFab) {
            requireActivity().addMenuProvider(this, viewLifecycleOwner)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fileRecyclerView.apply {
            val gridLayoutManager = GridLayoutManager(requireContext(), 2)
            layoutManager = gridLayoutManager
            adapter = fileSelectorAdapter
        }

        viewModel.localFileLiveData.observe(viewLifecycleOwner) {
            binding.root.isRefreshing = false
            binding.progressBar.isInvisible = true
            files.clear()
            files.addAll(it)
            fileSelectorAdapter.notifyDataSetChanged()
            binding.fileRecyclerView.scheduleLayoutAnimation()

            fileSelectorAdapter.update()
        }

        lifecycleScope.launchWhenResumed {
            viewModel.getLocalFiles(requireContext(), args.path)
            navController.addOnDestinationChangedListener(destinationChangedListener)
        }

        binding.root.setOnRefreshListener {
            viewModel.refresh(requireContext(), args.path)
        }
    }

    override fun onFilter(key: String?) {
        fileSelectorAdapter.filter.filter(key)
    }

    override fun scrollToFirst() {
        if (_binding == null) return
        binding.fileRecyclerView.scrollToPosition(0)
    }

    /**
     * 当从上传列表返回时，这里将被调用，检查文件是否是已选择的，如未选择，更新状态并删除已选择数据
     */
    private val destinationChangedListener = NavController.OnDestinationChangedListener { _, b, _ ->
        // changed
        if (!isVisible) return@OnDestinationChangedListener
        if (fileViewModel.selectedList.isEmpty()) return@OnDestinationChangedListener
        val iterator = fileViewModel.selectedList.iterator()
        while (iterator.hasNext()) {
            val fileInfo = iterator.next()
            if (!fileInfo.isSelected) {
                val index = files.indexOf(fileInfo)
                if (index == -1) continue
                fileSelectorAdapter.updateItem(index, 0)
                iterator.remove()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        requireActivity().removeMenuProvider(this)
        navController.removeOnDestinationChangedListener(destinationChangedListener)
    }
}