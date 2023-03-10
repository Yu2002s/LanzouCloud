package cc.drny.lanzou.ui.upload

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.core.view.isInvisible
import androidx.documentfile.provider.DocumentFile
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
import cc.drny.lanzou.databinding.FragmentAndroidDataBinding
import cc.drny.lanzou.databinding.ItemListFileSelectorBinding
import cc.drny.lanzou.databinding.ItemListFolderBinding
import cc.drny.lanzou.event.FileFilterable
import cc.drny.lanzou.event.OnItemClickListener
import cc.drny.lanzou.event.Scrollable
import cc.drny.lanzou.ui.file.FileFragmentDirections
import cc.drny.lanzou.util.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class AndroidDataFragment : BaseSuperFragment(), FileFilterable, MenuProvider, Scrollable {

    private var _binding: FragmentAndroidDataBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<AndroidDataViewModel> {
        AndroidDataViewModel.AndroidDataViewModelFactory(fileViewModel.selectedList)
    }
    private val fileViewModel by navGraphViewModels<UploadSelectorViewModel>(R.id.uploadFileFragment)

    private val files = mutableListOf<FileInfo>()
    private val fileAdapter = FileSelectorAdapter(files)

    private val args by navArgs<AndroidDataFragmentArgs>()

    private lateinit var register: ActivityResultLauncher<String>

    private lateinit var navController: NavController

    private var isShowFab = false

    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navController = findNavController()
        isShowFab = navController.currentDestination!!.id == R.id.uploadFileFragment
        register = registerForActivityResult(RequestAccessAppDataDir()) {
            val uri = it?.data
            if (uri != null) {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                lifecycleScope.launch {
                    viewModel.getFiles(
                        requireContext(),
                        DocumentFile.fromTreeUri(requireContext(), uri)!!
                    )
                }
            } else {
                "用户未授权".showToast()
                binding.progressbar.isInvisible = true
            }
        }

        fileAdapter.enableAutoLoad().scopeIn(lifecycleScope)
        fileAdapter.onItemClickListener =
            object : OnItemClickListener<FileInfo, ViewBinding> {
                override fun onItemClick(position: Int, data: FileInfo, binding: ViewBinding) {
                    if (binding is ItemListFileSelectorBinding) {
                        data.isSelected = !data.isSelected
                        binding.root.isChecked = data.isSelected
                        if (data.isSelected) {
                            fileViewModel.addSelect(data)
                        } else {
                            fileViewModel.removeSelect(data)
                        }
                    } else if (binding is ItemListFolderBinding) {
                        navController.navigate(
                            AndroidDataFragmentDirections
                                .actionGlobalAndroidDataFragment(data.path, data.name)
                        )
                    }
                }
            }
    }

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
                navController.navigate(
                    FileFragmentDirections.actionGlobalUploadFileDialogFragment()
                )
                true
            }
            else -> false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAndroidDataBinding.inflate(inflater, container, false)
        if (!isShowFab) {
            requireActivity().addMenuProvider(this, viewLifecycleOwner)
        }
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fileRecyclerView.apply {
            val gridLayoutManager = GridLayoutManager(requireContext(), 2)
            layoutManager = gridLayoutManager
            adapter = fileAdapter
        }

        viewModel.fileLiveData.observe(viewLifecycleOwner) {
            binding.root.isRefreshing = false
            binding.progressbar.isInvisible = true
            files.clear()
            files.addAll(it)
            fileAdapter.notifyDataSetChanged()

            fileAdapter.update()
        }

        val uri = args.path.path2Uri()

        lifecycleScope.launchWhenResumed {
            navController.addOnDestinationChangedListener(destinationChangedListener)
            val documentFile =
                DocumentFile.fromTreeUri(requireContext(), uri) ?: return@launchWhenResumed
            if (documentFile.canRead()) {
                viewModel.getFiles(requireContext(), documentFile)
            } else {
                MaterialAlertDialogBuilder(requireContext()).apply {
                    setCancelable(false)
                    setTitle("提示")
                    setMessage("由于Android系统限制，访问应用私有目录需要得到你的授权，请点击去授权，然后点击使用此文件夹")
                    setPositiveButton("去授权") { _, _ ->
                        register.launch(args.path)
                    }
                    setNegativeButton("拒绝") { _, _ ->
                        binding.progressbar.isInvisible = true
                    }
                    show()
                }
            }
        }

        binding.root.setOnRefreshListener {
            viewModel.refreshFiles(
                requireContext(),
                DocumentFile.fromTreeUri(requireContext(), uri)!!
            )
        }

    }

    override fun onFilter(key: String?) {
        fileAdapter.filter.filter(key)
    }

    override fun scrollToFirst() {
        binding.fileRecyclerView.scrollToPosition(0)
    }

    /**
     * 当从上传列表返回时，这里将被调用，检查文件是否是已选择的，如未选择，更新状态并删除已选择数据
     */
    private val destinationChangedListener = NavController.OnDestinationChangedListener { _, _, _ ->
        // changed
        if (!isVisible) return@OnDestinationChangedListener
        if (fileViewModel.selectedList.isEmpty()) return@OnDestinationChangedListener
        val iterator = fileViewModel.selectedList.iterator()
        while (iterator.hasNext()) {
            val fileInfo = iterator.next()
            if (!fileInfo.isSelected) {
                val index = files.indexOf(fileInfo)
                if (index == -1) continue
                fileAdapter.updateItem(index, 0)
                iterator.remove()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        register.unregister()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        navController.removeOnDestinationChangedListener(destinationChangedListener)
        requireActivity().removeMenuProvider(this)
        _binding = null
    }

}