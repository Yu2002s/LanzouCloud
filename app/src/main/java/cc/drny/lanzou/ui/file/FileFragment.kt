package cc.drny.lanzou.ui.file

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.view.*
import androidx.activity.addCallback
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
        fileAdapter.onItemClickListener = object : OnItemClickListener<LanzouFile, ViewBinding> {
            override fun onItemClick(position: Int, v: View) {
                val lanzouFile = lanzouFiles[position]
                if (isMultiMode) {
                    lanzouFile.isSelected = !lanzouFile.isSelected
                    (v as MaterialCardView).isChecked = lanzouFile.isSelected
                    if (lanzouFile.isSelected) {
                        selectedCount++
                    } else {
                        selectedCount--
                    }
                    return
                }
                if (lanzouFile.isFolder()) {
                    val navController = findNavController()
                    navController.navigate(
                        FileFragmentDirections.actionFileFragmentSelf(
                            LanzouPage(lanzouFile.folderId, lanzouFile.name),
                            lanzouFile.name
                        )
                    )
                } else {
                    showFileActionMenu(v, lanzouFile, position)
                }
            }
        }
        fileAdapter.onItemLongClickListener =
            object : OnItemLongClickListener<LanzouFile, ViewBinding> {
                override fun onItemLongClick(position: Int, v: View) {
                    val lanzouFile = lanzouFiles[position]
                    if (lanzouFile.isSelected) {
                        showFileActionMenu(v, lanzouFile, position)
                    } else {
                        selectedCount++
                        lanzouFile.isSelected = true
                        (v as MaterialCardView).isChecked = true
                    }
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFileBinding.inflate(inflater, container, false)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                        lanzouFiles.removeAt(i)
                        fileAdapter.notifyItemRemoved(i)
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

    private fun getFiles() {
        viewModel.getFiles(binding.fileRecyclerView)
    }

    private fun showFileActionMenu(v: View, lanzouFile: LanzouFile, position: Int) {
        FileActionHelper.showFileActionPopupMenu(
            this, downloadService, downloadViewModel, fileAdapter, lanzouFiles,
            v, lanzouFile, position, lanzouPage, clipboardManager
        ) {
            selectedCount = 0
        }
    }

    private fun cancelMultiMode() {
        selectedCount = 0
        lanzouFiles.forEachIndexed { index, lanzouFile ->
            if (lanzouFile.isSelected) {
                lanzouFile.isSelected = false
                fileAdapter.updateItem(index, BaseAdapter.NOTIFY_KEY)
            }
        }
    }

    override fun onCreateMenu(p0: Menu, p1: MenuInflater) {
        FileActionHelper.createSearchMenuItem(p0, p1, fileAdapter)
    }

    override fun onMenuItemSelected(p0: MenuItem): Boolean {
        return FileActionHelper.initMenu(this, p0, lanzouPage, clipboardManager)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        downloadService = (service as DownloadService.DownloadBinder).getService()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().unbindService(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        requireActivity().removeMenuProvider(this)
    }
}