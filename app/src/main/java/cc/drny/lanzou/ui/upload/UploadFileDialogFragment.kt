package cc.drny.lanzou.ui.upload

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import cc.drny.lanzou.R
import cc.drny.lanzou.adapter.FileSelectorAdapter
import cc.drny.lanzou.data.lanzou.LanzouFolder
import cc.drny.lanzou.data.upload.FileInfo
import cc.drny.lanzou.databinding.DialogUploadFileBinding
import cc.drny.lanzou.databinding.ItemListFileSelectorBinding
import cc.drny.lanzou.event.OnItemClickListener
import cc.drny.lanzou.network.LanzouRepository
import cc.drny.lanzou.service.UploadService
import cc.drny.lanzou.util.getWindowHeight
import cc.drny.lanzou.util.showToast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UploadFileDialogFragment : BottomSheetDialogFragment(), ServiceConnection {

    private var _binding: DialogUploadFileBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<UploadFileDialogFragmentArgs>()

    private lateinit var viewModel: UploadSelectorViewModel
    private val uploadViewModel by navGraphViewModels<UploadViewModel>(R.id.fileFragment)

    private lateinit var selectorAdapter: FileSelectorAdapter

    private var uploadService: UploadService? = null

    private val mmkv = MMKV.defaultMMKV()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireContext().bindService(
            Intent(requireContext(), UploadService::class.java),
            this,
            Context.BIND_AUTO_CREATE
        )
        val destinationId = if (args.external) R.id.fileFragment else R.id.uploadFileFragment
        val backStackEntry = findNavController().getBackStackEntry(destinationId)
        val store = backStackEntry.viewModelStore
        viewModel = ViewModelProvider(
            store,
            backStackEntry.defaultViewModelProviderFactory,
            backStackEntry.defaultViewModelCreationExtras
        )[UploadSelectorViewModel::class.java]
        if (args.external) {
            args.files?.forEach {
                viewModel.addFirstSelect(it)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val frameLayout =
            dialog!!.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        frameLayout!!.layoutParams.height = -1
        val behavior = (dialog as BottomSheetDialog).behavior
        behavior.peekHeight = (getWindowHeight() * 0.7).toInt()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogUploadFileBinding.inflate(inflater, container, false)

        binding.selectorRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            selectorAdapter = FileSelectorAdapter(viewModel.selectedList)
            if (args.external) {
                selectorAdapter.enableAutoLoad()
                    .scopeIn(lifecycleScope)
            }
            selectorAdapter.onItemClickListener =
                object : OnItemClickListener<FileInfo, ViewBinding> {
                    override fun onItemClick(data: FileInfo, v: View) {
                        data.isSelected = !data.isSelected
                        (v as MaterialCardView).isChecked = data.isSelected
                    }
                }
            adapter = selectorAdapter
            if (args.external) {
                selectorAdapter.update()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()

        val folderList = mutableListOf<LanzouFolder>()
        lifecycleScope.launch {
            val folders = withContext(Dispatchers.IO) {
                LanzouRepository.getAllFolder()
                    ?.apply { folderList.addAll(this) }
                    ?.map { it.folder_name }
                    ?.toTypedArray() ?: arrayOf("根目录")
            }
            (binding.textField.editText as MaterialAutoCompleteTextView)
                .setSimpleItems(folders)
            val path = mmkv.getString("upload_path", null)
            if (path != null) {
                binding.textField.editText!!.setText(path)
            }
        }

        binding.btnUpload.setOnClickListener {
            if (viewModel.selectedList.isEmpty()) return@setOnClickListener
            if (folderList.isEmpty()) return@setOnClickListener
            val editText = binding.textField.editText as MaterialAutoCompleteTextView
            val folderName = editText.text.toString()

            val index = folderList.indexOfFirst {
                it.folder_name == folderName
            }

            if (index == -1) {
                "请确认文件夹是否存在".showToast()
                return@setOnClickListener
            }

            val folderId = folderList[index].folder_id

            val iterator = viewModel.selectedList.iterator()
            while (iterator.hasNext()) {
                val fileInfo = iterator.next()
                if (fileInfo.isSelected) {
                    fileInfo.isSelected = false
                    uploadService!!.addUpload(fileInfo, folderId, folderName) { upload ->
                        Log.d("jdy", "upload: $fileInfo")
                        upload.icon = fileInfo.icon
                        uploadViewModel.addUpload(upload)
                    }
                }
                if (args.external) {
                    iterator.remove()
                }
            }
            if (args.external) {
                navController.previousBackStackEntry?.savedStateHandle?.set("isUpload", true)
            }
            navController.popBackStack()
        }

        binding.btnClear.setOnClickListener {
            viewModel.selectedList.forEachIndexed { index, fileInfo ->
                if (fileInfo.isSelected) {
                    fileInfo.isSelected = false
                    selectorAdapter.updateItem(index, 0)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mmkv.encode("upload_path", binding.textField.editText!!.text.toString())
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().unbindService(this)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        uploadService = (service as UploadService.UploadBinder).getService()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
    }
}