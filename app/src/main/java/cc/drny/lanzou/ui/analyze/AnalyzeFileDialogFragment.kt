package cc.drny.lanzou.ui.analyze

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import cc.drny.lanzou.R
import cc.drny.lanzou.adapter.ShareFileAdapter
import cc.drny.lanzou.data.lanzou.LanzouFile
import cc.drny.lanzou.data.lanzou.LanzouShareFile
import cc.drny.lanzou.databinding.DialogAnalyzeFileBinding
import cc.drny.lanzou.databinding.ItemListFileBinding
import cc.drny.lanzou.event.OnItemClickListener
import cc.drny.lanzou.network.LanzouRepository
import cc.drny.lanzou.service.DownloadService
import cc.drny.lanzou.ui.download.DownloadViewModel
import cc.drny.lanzou.util.LanzouAnalyzeException
import cc.drny.lanzou.util.getIcon
import cc.drny.lanzou.util.showToast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnalyzeFileDialogFragment : BottomSheetDialogFragment(), ServiceConnection {

    private var _binding: DialogAnalyzeFileBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<AnalyzeFileDialogFragmentArgs>()

    private val lanzouShareFiles = mutableListOf<LanzouShareFile>()

    private val shareFileAdapter = ShareFileAdapter(lanzouShareFiles)

    private lateinit var downloadService: DownloadService

    private val downloadViewModel by navGraphViewModels<DownloadViewModel>(R.id.fileFragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireContext().bindService(
            Intent(requireContext(), DownloadService::class.java),
            this,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        downloadService = (service as DownloadService.DownloadBinder).getService()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAnalyzeFileBinding.inflate(inflater, container, false)

        shareFileAdapter.onItemClickListener =
            object : OnItemClickListener<LanzouShareFile, ItemListFileBinding> {
                override fun onItemClick(
                    position: Int,
                    data: LanzouShareFile,
                    binding: ItemListFileBinding
                ) {
                    this@AnalyzeFileDialogFragment.binding.editUrl.setText(data.url)
                    this@AnalyzeFileDialogFragment.binding.editPwd.setText(data.pwd)
                    analyzeFile(data, position)
                }
            }
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val files = args.files

        files?.let {

            lanzouShareFiles.clear()
            lanzouShareFiles.addAll(it)

            if (lanzouShareFiles.isEmpty()) return@let
            binding.editUrl.setText(lanzouShareFiles[0].url)
            binding.editPwd.setText(lanzouShareFiles[0].pwd)

            if (lanzouShareFiles[0].pwd.isNullOrBlank()) return@let
            if (savedInstanceState != null) return@let
            analyzeFile(lanzouShareFiles[0], 0)
        }

        binding.analyzeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.analyzeRecyclerView.adapter = shareFileAdapter

        binding.btnAnalyze.setOnClickListener {
            val url = binding.editUrl.text.toString().trim()
            val pwd = binding.editPwd.text.toString().trim()
            analyzeFile(LanzouShareFile(url = url, pwd = pwd), -1)
        }
    }

    private fun analyzeFile(lanzouShareFile: LanzouShareFile, position: Int) {
        var index = position
        if (index == -1) {
            index = lanzouShareFiles.indexOf(lanzouShareFile)
            if (index == -1) {
                index = 0
                lanzouShareFiles.add(0, lanzouShareFile)
                shareFileAdapter.notifyItemInserted(0)
            } else {
                lanzouShareFiles[index] = lanzouShareFile
            }
        }
        lifecycleScope.launch {
            var lanzouFile: LanzouFile? = null
            val result = withContext(Dispatchers.IO) {
                LanzouRepository.getDownloadUrl(lanzouShareFile.url, lanzouShareFile.pwd) {
                    lanzouShareFile.id = it.fileId
                    lanzouShareFile.name = it.name_all
                    lanzouShareFile.extension = it.icon
                    lanzouShareFile.desc = it.size + " - " + it.time
                    it.iconDrawable = it.icon.getIcon(requireContext())
                    lanzouShareFile.icon = it.iconDrawable
                    lanzouFile = it
                }
            }
            result.onSuccess {
                shareFileAdapter.notifyItemChanged(index)
                MaterialAlertDialogBuilder(requireContext()).apply {
                    setTitle(lanzouShareFile.name)
                    setCancelable(false)
                    setMessage(lanzouShareFile.desc + "\n是否立即下载该文件")
                    setPositiveButton("下载") { _, _ ->
                        downloadService.addDownload(lanzouFile!!, lanzouShareFile) {
                            downloadViewModel.addDownload(it)
                        }
                    }
                    setNegativeButton("取消", null)
                    show()
                }
            }.onFailure {
                if (it is LanzouAnalyzeException) {
                    // 解析文件夹
                    try {
                        findNavController().navigate(AnalyzeFileDialogFragmentDirections
                            .actionAnalyzeFileDialogFragmentToAnalyzeFolderDialogFragment(lanzouShareFile))
                    } catch (_: Exception) {
                    }
                    return@onFailure
                }
                it.message.showToast()
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