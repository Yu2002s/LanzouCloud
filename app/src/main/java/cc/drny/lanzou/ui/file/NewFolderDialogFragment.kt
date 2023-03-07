package cc.drny.lanzou.ui.file

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import cc.drny.lanzou.databinding.DialogNewFolderBinding
import cc.drny.lanzou.network.LanzouRepository
import cc.drny.lanzou.util.showToast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewFolderDialogFragment: BottomSheetDialogFragment() {

    private var _binding: DialogNewFolderBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<NewFolderDialogFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogNewFolderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()

        binding.btnCancle.setOnClickListener {
            navController.popBackStack()
        }

        binding.btnNewFolder.setOnClickListener {
            val name = binding.editName.text.toString()
            val desc = binding.editDesc.text.toString()

            if (name.isEmpty()) return@setOnClickListener

            // 开始新建文件夹
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    LanzouRepository.newFolder(args.folderId, name, desc)
                }
                result.onSuccess {
                    it.showToast()
                    val previousBackStackEntry = navController.previousBackStackEntry
                    previousBackStackEntry?.savedStateHandle?.set("newFolder", name)
                    navController.popBackStack()
                }.onFailure {
                    it.message.showToast()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}