package cc.drny.lanzou.ui.analyze

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.addCallback
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isInvisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewbinding.ViewBinding
import cc.drny.lanzou.R
import cc.drny.lanzou.adapter.BaseAdapter
import cc.drny.lanzou.adapter.FileAdapter
import cc.drny.lanzou.data.lanzou.LanzouFile
import cc.drny.lanzou.data.lanzou.LanzouShareFile
import cc.drny.lanzou.databinding.DialogAnalyzeFolderBinding
import cc.drny.lanzou.event.OnItemClickListener
import cc.drny.lanzou.event.OnItemLongClickListener
import cc.drny.lanzou.network.LanzouRepository
import cc.drny.lanzou.service.DownloadService
import cc.drny.lanzou.ui.download.DownloadViewModel
import cc.drny.lanzou.util.findLastVisiblePosition
import cc.drny.lanzou.util.getWindowHeight
import cc.drny.lanzou.util.showToast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnalyzeFolderDialogFragment : BottomSheetDialogFragment(), ServiceConnection {

    private var _binding: DialogAnalyzeFolderBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<AnalyzeFolderDialogFragmentArgs>()

    private val viewModel by viewModels<AnalyzeFolderViewModel>()
    private val downloadViewModel by navGraphViewModels<DownloadViewModel>(R.id.fileFragment)

    private val lanzouFiles = mutableListOf<LanzouFile>()
    private val fileAdapter = FileAdapter(lanzouFiles)

    private lateinit var downloadService: DownloadService

    private var isMultiMode = false
    private var selectedCount = 0

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        downloadService = (service as DownloadService.DownloadBinder).getService()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
    }

    override fun onStart() {
        super.onStart()
        val frameLayout =
            dialog!!.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        frameLayout!!.layoutParams.height = -1
        val behavior = (dialog as BottomSheetDialog).behavior
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.peekHeight = (getWindowHeight() * 0.7f).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setStyle(STYLE_NO_FRAME, R.style.AppTheme)
        requireContext().bindService(
            Intent(requireContext(), DownloadService::class.java),
            this,
            Context.BIND_AUTO_CREATE
        )
        fileAdapter.onItemLongClickListener =
            object : OnItemLongClickListener<LanzouFile, ViewBinding> {
                override fun onItemLongClick(
                    data: LanzouFile,
                    position: Int,
                    binding: ViewBinding
                ) {
                    if (data.isSelected) {
                        showPopupMenu(data, binding.root)
                    } else {
                        selectedCount++
                        data.isSelected = true
                        (binding.root as MaterialCardView).isChecked = true
                        if (!isMultiMode) {
                            isMultiMode = true
                        }
                    }
                }
            }
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
                    if (selectedCount == 0) {
                        isMultiMode = false
                    }
                    return
                }
                showPopupMenu(data, binding.root)
            }
        }
    }

    private fun showPopupMenu(lanzouFile: LanzouFile, v: View) {
        val popupMenu = PopupMenu(requireContext(), v, Gravity.END)
        val menuItem = popupMenu.menu.add("下载")
        menuItem.setOnMenuItemClickListener {
            if (lanzouFile.isSelected) {
                isMultiMode = false
                lifecycleScope.launch {
                    val i = args.share.url.lastIndexOf("/") + 1
                    lanzouFiles.forEachIndexed { index, lanzouFile ->
                        val host = args.share.url.substring(0, i) + lanzouFile.fid
                        if (lanzouFile.isSelected) {
                            withContext(Dispatchers.IO) {
                                LanzouRepository.getDownloadUrl(host) {
                                    lanzouFile.fileId = it.fileId
                                }
                            }.onSuccess {
                                downloadService.addDownload(
                                    lanzouFile, LanzouShareFile(
                                        host, downloadUrl = it
                                    )
                                ) { download ->
                                    downloadViewModel.addDownload(download)
                                }
                                lanzouFile.isSelected = false
                                fileAdapter.updateItem(index, BaseAdapter.NOTIFY_KEY)
                            }
                        }
                    }
                }
            } else {
                lifecycleScope.launch {
                    val index = args.share.url.lastIndexOf("/") + 1
                    val host = args.share.url.substring(0, index) + lanzouFile.fid
                    withContext(Dispatchers.IO) {
                        LanzouRepository.getDownloadUrl(host) {
                            lanzouFile.fileId = it.fileId
                        }
                    }.onSuccess {
                        downloadService.addDownload(
                            lanzouFile, LanzouShareFile(
                                host, downloadUrl = it
                            )
                        ) { download ->
                            downloadViewModel.addDownload(download)
                        }
                    }.onFailure {
                        it.message.showToast()
                        Log.e("jdy", it.message.toString())
                    }
                }
            }
            true
        }
        popupMenu.show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAnalyzeFolderBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fileRecyclerView.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter = fileAdapter
            addOnScrollListener(object : OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val last = recyclerView.findLastVisiblePosition()
                    if (last == -1) return
                    if (last >= recyclerView.adapter!!.itemCount - 6) {
                        viewModel.getFolderList(requireContext(), args.share)
                    }
                }
            })
        }

        viewModel.fileLiveData.observe(viewLifecycleOwner) {
            binding.progressbar.isInvisible = true
            val start = lanzouFiles.size
            lanzouFiles.clear()
            lanzouFiles.addAll(it)
            if (viewModel.isFirstPage()) {
                fileAdapter.notifyDataSetChanged()
            } else {
                fileAdapter.notifyItemRangeInserted(start, lanzouFiles.size)
            }
        }

        if (savedInstanceState == null) {
            viewModel.getFolderList(requireContext(), args.share)
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (isMultiMode) {
                isMultiMode = false
                cancelMultiMode()
            } else {
                if (!findNavController().navigateUp()) {
                    requireActivity().moveTaskToBack(false)
                }
            }
        }

        binding.editSearch.doOnTextChanged { text, _, _, _ ->
            fileAdapter.filter.filter(text)
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

    override fun onDestroy() {
        super.onDestroy()
        requireContext().unbindService(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}