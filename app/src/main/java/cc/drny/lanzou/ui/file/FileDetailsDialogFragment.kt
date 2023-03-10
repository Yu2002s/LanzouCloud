package cc.drny.lanzou.ui.file

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import cc.drny.lanzou.R
import cc.drny.lanzou.databinding.DialogDetailsFileBinding
import cc.drny.lanzou.network.LanzouRepository
import cc.drny.lanzou.util.showToast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class FileDetailsDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogDetailsFileBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<FileDetailsDialogFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDetailsFileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = findNavController()

        binding.editFileName.setText(args.fileName)
        if (!args.isFile) {
            binding.editFileName.isEnabled = true
        }

        var password: String? = ""
        var desc = ""
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (args.isFile) {
                    val result = LanzouRepository.getFileInfo(args.fileId)
                    val result2 = LanzouRepository.getFileDescribe(args.fileId)
                    val lanzouUrl = result.getOrThrow()
                    val describe = result2.getOrThrow()
                    desc = describe
                    withContext(Dispatchers.Main) {
                        binding.editPwd.setText(lanzouUrl.pwd)
                        password = lanzouUrl.pwd
                        binding.editDesc.setText(describe)
                        binding.cbPwd.isChecked = lanzouUrl.hasPwd == 1
                        showQrCode(LanzouRepository.getShareUrl(lanzouUrl))
                    }
                } else {
                    val result = LanzouRepository.getFolder(args.fileId)
                    val lanzouUrl = result.getOrThrow()
                    withContext(Dispatchers.Main) {
                        binding.editPwd.setText(lanzouUrl.pwd)
                        password = lanzouUrl.pwd
                        desc = lanzouUrl.describe
                        binding.editDesc.setText(lanzouUrl.describe)
                        binding.cbPwd.isChecked = lanzouUrl.hasPwd == 1
                        showQrCode(lanzouUrl.url)
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    e.message.showToast()
                    navController.popBackStack()
                }
            }
        }

        binding.btnCancel.setOnClickListener {
            navController.popBackStack()
        }

        binding.btnSave.setOnClickListener {
            val describe = binding.editDesc.text.toString()
            lifecycleScope.launch(Dispatchers.IO) {
                val name = binding.editFileName.text.toString()
                try {
                    if (args.isFile) {
                        if (desc != describe) {
                            LanzouRepository.saveFileDescribe(args.fileId, describe).getOrThrow()
                        }
                    } else {
                        if (desc != describe || name != args.fileName) {
                            LanzouRepository.saveFolderDescribe(args.fileId, name, describe)
                                .getOrThrow()
                        }
                    }
                    val pwd = binding.editPwd.text.toString()
                    if (pwd != password) {
                        val enablePwd = binding.cbPwd.isChecked
                        if (args.isFile) {
                            LanzouRepository.editFilePassword(args.fileId, enablePwd, pwd)
                                .getOrThrow()
                        } else {
                            LanzouRepository.editFolderPassword(args.fileId, enablePwd, pwd)
                                .getOrThrow()
                        }
                    }
                    withContext(Dispatchers.Main) {
                        "保存成功".showToast()
                        if (args.fileName != name) {
                            // 修改文件夹的名称
                            navController.previousBackStackEntry?.savedStateHandle?.set(
                                "editFolder",
                                name
                            )
                        }
                        navController.popBackStack()
                    }
                } catch (e: Throwable) {
                    Log.e("jdy", e.toString())
                    withContext(Dispatchers.Main) {
                        e.message.showToast()
                    }
                }
            }
        }

        binding.cbPwd.setOnClickListener {
            val pwd = binding.editPwd.text.toString()
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    if (args.isFile) {
                        LanzouRepository.editFilePassword(args.fileId, binding.cbPwd.isChecked, pwd)
                    } else {
                        LanzouRepository.editFolderPassword(
                            args.fileId,
                            binding.cbPwd.isChecked,
                            pwd
                        )
                    }
                }.onSuccess {
                    it.showToast()
                }.onFailure {
                    it.message.showToast()
                    binding.cbPwd.isChecked = !binding.cbPwd.isChecked
                }
            }
        }
    }

    private fun showQrCode(content: String) {
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(content, BarcodeFormat.QR_CODE, 400, 400)
            binding.qrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            binding.qrCode.setImageResource(R.mipmap.ic_launcher)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}