package cc.drny.lanzou.ui.file

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.view.*
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.core.view.isInvisible
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewbinding.ViewBinding
import cc.drny.lanzou.R
import cc.drny.lanzou.adapter.BaseAdapter
import cc.drny.lanzou.adapter.FileAdapter
import cc.drny.lanzou.base.BaseSuperFragment
import cc.drny.lanzou.data.lanzou.LanzouFile
import cc.drny.lanzou.data.lanzou.LanzouPage
import cc.drny.lanzou.data.state.Empty
import cc.drny.lanzou.data.state.Error
import cc.drny.lanzou.data.state.Loading
import cc.drny.lanzou.databinding.FragmentFileBinding
import cc.drny.lanzou.event.OnItemClickListener
import cc.drny.lanzou.event.OnItemLongClickListener
import cc.drny.lanzou.service.DownloadService
import cc.drny.lanzou.ui.download.DownloadViewModel
import cc.drny.lanzou.util.enableMenuIcon
import cc.drny.lanzou.util.findLastVisiblePosition
import com.google.android.material.card.MaterialCardView

class FileFragment : BaseSuperFragment(), ServiceConnection, MenuProvider {

    private var _binding: FragmentFileBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<FileFragmentArgs>()

    private val lanzouPage get() = args.lanzouPage ?: LanzouPage()

    private val viewModel by viewModels<FileViewModel> {
        FileViewModel.FileViewModelFactory(lanzouPage)
    }

    private val downloadViewModel by navGraphViewModels<DownloadViewModel>(R.id.fileFragment)

    private lateinit var downloadService: DownloadService

    private val lanzouFiles get() = viewModel.lanzouFiles
    private lateinit var fileAdapter: FileAdapter

    private val clipboardManager by lazy {
        requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    private var searchView: SearchView? = null

    private val isMultiMode
        get() = selectedCount > 0

    private var selectedCount
        get() = viewModel.selectedCount
        set(value) {
            viewModel.selectedCount = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireContext().bindService(
            Intent(requireContext(), DownloadService::class.java),
            this,
            Context.BIND_AUTO_CREATE
        )
        fileAdapter = FileAdapter(lanzouFiles)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFileBinding.inflate(inflater, container, false)

        addMenuProvider(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fileAdapter.searchKeyWord = viewModel.key

        initView()
        getFiles()

        viewModel.uiState.observe(viewLifecycleOwner) {
            binding.tvLoad.isInvisible = !(it is Error || it is Empty)
            binding.root.isRefreshing = false
            binding.progressbar.isInvisible = it !is Loading
            when (it) {
                is Error -> {
                    // 加载出错了
                    binding.tvLoad.text = it.error
                }

                is Empty -> {
                    binding.tvLoad.text = "这里没有文件"
                }

                else -> {}
            }
        }

        val navController = findNavController()
        observeLiveData(navController)
        handleBack(navController)
        initAdapterEvent()
    }

    private fun initView() {
        binding.fileRecyclerView.apply {
            val gridLayoutManager =
                StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            layoutManager = gridLayoutManager
            adapter = fileAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val last = recyclerView.findLastVisiblePosition()
                    if (last >= fileAdapter.itemCount - 6) {
                        viewModel.loadMore(recyclerView)
                    }
                }
            })
        }

        binding.root.setOnRefreshListener {
            selectedCount = 0
            viewModel.refresh(binding.fileRecyclerView)
        }
    }

    private fun observeLiveData(navController: NavController) {
        val stateHandle = navController.currentBackStackEntry?.savedStateHandle
        stateHandle?.getLiveData<Long>("deleteFolder")
            ?.observe(viewLifecycleOwner) { id ->
                stateHandle.remove<Long>("deleteFolder")
                val position = lanzouFiles.indexOfFirst {
                    it.folderId == id
                }
                lanzouFiles.removeAt(position)
                fileAdapter.notifyItemRemoved(position)
            }
        stateHandle?.getLiveData<String>("newFolder")
            ?.observe(viewLifecycleOwner) { folderName ->
                stateHandle.remove<String>("newFolder")
                lanzouFiles.add(0, LanzouFile(folderName))
                fileAdapter.notifyItemInserted(0)
            }
        stateHandle?.getLiveData<List<Long>>("moveFile")
            ?.observe(viewLifecycleOwner) { ids ->
                stateHandle.remove<List<Long>>("moveFile")
                selectedCount = 0
                var count = ids.size
                for (i in lanzouFiles.size - 1 downTo 0) {
                    val lanzouFile = lanzouFiles[i]
                    if (ids.contains(lanzouFile.fileId)) {
                        fileAdapter.removeItems(i, lanzouFile)
                        if (--count == 0) {
                            break
                        }
                    }
                }
            }
        stateHandle?.getLiveData<Boolean>("isLogin")
            ?.observe(viewLifecycleOwner) {
                stateHandle.remove<Boolean>("isLogin")
                viewModel.refresh(binding.fileRecyclerView)
            }
        stateHandle?.getLiveData<String>("editFolder")
            ?.observe(viewLifecycleOwner) {
                stateHandle.remove<String>("editFolder")
                lanzouPage.name = it
                navController.currentDestination?.label = it
                arguments?.putString("name", it)
                mainActivity.supportActionBar?.title = it
            }
    }

    private fun handleBack(navController: NavController) {
        val onBackPressedDispatcher = requireActivity().onBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this) {
            if (isMultiMode) {
                cancelMultiMode()
            } else {
                if (!navController.navigateUp()) {
                    requireActivity().moveTaskToBack(false)
                }
            }
        }
    }

    private fun initAdapterEvent() {
        fileAdapter.onItemClickListener = object : OnItemClickListener<LanzouFile, ViewBinding> {
            override fun onItemClick(position: Int, data: LanzouFile, binding: ViewBinding) {
                if (isMultiMode) {
                    data.isSelected = !data.isSelected
                    (binding.root as MaterialCardView).isChecked = data.isSelected
                    if (data.isSelected) {
                        selectedCount++
                    } else {
                        selectedCount--
                    }
                    return
                }
                if (data.isFolder()) {
                    val navController = findNavController()
                    navController.navigate(
                        FileFragmentDirections.actionFileFragmentSelf(
                            LanzouPage(data.folderId, data.name),
                            data.name
                        )
                    )
                } else {
                    showFileActionMenu(binding.root, data, position)
                }
            }
        }
        fileAdapter.onItemLongClickListener =
            object : OnItemLongClickListener<LanzouFile, ViewBinding> {
                override fun onItemLongClick(
                    data: LanzouFile,
                    position: Int,
                    binding: ViewBinding
                ) {
                    if (data.isSelected) {
                        showFileActionMenu(binding.root, data, position)
                    } else {
                        selectedCount++
                        data.isSelected = true
                        (binding.root as MaterialCardView).isChecked = true
                    }
                }
            }
    }

    private fun getFiles() {
        viewModel.getFiles(binding.fileRecyclerView)
    }

    private fun showFileActionMenu(v: View, lanzouFile: LanzouFile, position: Int) {
        FileActionHelper.showFileActionPopupMenu(
            this, downloadService, downloadViewModel, fileAdapter, lanzouFiles,
            v, lanzouFile, position, lanzouPage, clipboardManager, searchView
        ) {
            selectedCount = 0
        }
    }

    private fun cancelMultiMode() {
        selectedCount = 0
        fileAdapter.getList().forEachIndexed { index, lanzouFile ->
            if (lanzouFile.isSelected) {
                lanzouFile.isSelected = false
                fileAdapter.updateItem(index, lanzouFile, BaseAdapter.NOTIFY_KEY)
            }
        }
    }

    override fun onCreateMenu(p0: Menu, p1: MenuInflater) {
        p0.enableMenuIcon()
        p1.inflate(R.menu.menu_folder_action, p0)
        val searchItem = p0.findItem(R.id.search_file)
        searchView = searchItem.actionView as SearchView
        if (fileAdapter.searchKeyWord.isNotEmpty()) {
            searchItem.expandActionView()
            searchView?.setQuery(viewModel.key, false)
        }
        FileActionHelper.createSearchMenuItem(searchView, fileAdapter, viewModel)
    }

    override fun onMenuItemSelected(p0: MenuItem): Boolean {
        return FileActionHelper.initMenu(this, p0, lanzouPage, clipboardManager, searchView)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        downloadService = (service as DownloadService.DownloadBinder).getService()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
    }

    override fun onStop() {
        super.onStop()
        viewModel.key = fileAdapter.searchKeyWord
        searchView?.setOnQueryTextListener(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().unbindService(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchView = null
        _binding = null
    }
}