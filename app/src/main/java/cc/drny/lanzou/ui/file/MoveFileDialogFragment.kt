package cc.drny.lanzou.ui.file

import android.app.Dialog
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import cc.drny.lanzou.data.lanzou.LanzouFolder
import cc.drny.lanzou.databinding.DialogMoveFileBinding
import cc.drny.lanzou.network.LanzouRepository
import cc.drny.lanzou.util.showToast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.dialog.MaterialDialogs
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MoveFileDialogFragment: AppCompatDialogFragment() {

    private var _binding: DialogMoveFileBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<MoveFileDialogFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogMoveFileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext()).show().apply {
            dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lanzouPage = args.lanzouPage
        binding.editNowFolder.setText(lanzouPage.name)

        if (args.fileIds.size > 1) {
            binding.tvTitle.text = "移动${args.fileIds.size}个文件"
        }

        val folderList = mutableListOf<LanzouFolder>()
        lifecycleScope.launch {
            val folders = withContext(Dispatchers.IO) {
                LanzouRepository.getAllFolder()
                    ?.apply { folderList.addAll(this) }
                    ?.map { it.folder_name }
                    ?.toTypedArray() ?: arrayOf("根目录")
            }
            binding.editCurrentFolder.setSimpleItems(folders)
        }

        val navController = findNavController()
        binding.btnCancel.setOnClickListener {
            navController.popBackStack()
        }

        binding.btnMoveFile.setOnClickListener {
            if (folderList.isEmpty()) return@setOnClickListener
            val folderName = binding.editCurrentFolder.text.toString()

            val index = folderList.indexOfFirst {
                it.folder_name == folderName
            }

            if (index == -1) {
                "请确认文件夹是否存在".showToast()
                return@setOnClickListener
            }

            val folderId = folderList[index].folder_id

            lifecycleScope.launch {
                val moveFiles = mutableListOf<Long>()
                args.fileIds.forEach { id ->
                    withContext(Dispatchers.IO) {
                        LanzouRepository.moveFile(id, folderId)
                    }.onSuccess {
                        // it.showToast()
                        moveFiles.add(id)
                    }.onFailure {
                        it.message.showToast()
                    }
                }
                val previousBackStackEntry = navController.previousBackStackEntry
                previousBackStackEntry?.savedStateHandle?.set("moveFile", moveFiles)

                navController.popBackStack()
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}