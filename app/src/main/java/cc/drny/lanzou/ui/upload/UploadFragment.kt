package cc.drny.lanzou.ui.upload

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import cc.drny.lanzou.R
import cc.drny.lanzou.adapter.UploadAdapter
import cc.drny.lanzou.base.BaseUploadFragment
import cc.drny.lanzou.data.upload.Completed
import cc.drny.lanzou.data.upload.Error
import cc.drny.lanzou.data.upload.Insert
import cc.drny.lanzou.data.upload.Progress
import cc.drny.lanzou.data.upload.Stop
import cc.drny.lanzou.data.upload.Upload
import cc.drny.lanzou.data.upload.UploadState
import cc.drny.lanzou.databinding.FragmentUploadBinding
import cc.drny.lanzou.databinding.ItemListUploadBinding
import cc.drny.lanzou.event.FileFilterable
import cc.drny.lanzou.event.OnItemClickListener
import cc.drny.lanzou.event.Scrollable
import cc.drny.lanzou.ui.TransmissionFragment
import cc.drny.lanzou.util.*
import cc.drny.lanzou.util.DateUtils.handleTime
import cc.drny.lanzou.util.FileUtils.toSize
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File

class UploadFragment: BaseUploadFragment(), FileFilterable, Scrollable {

    private val uploadViewModel by navGraphViewModels<UploadViewModel>(R.id.fileFragment)

    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!

    private val uploadList = mutableListOf<Upload>()
    private val uploadAdapter = UploadAdapter(uploadList)

    private val clipboardManager by lazy {
        requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uploadAdapter.enableAutoLoad().scopeIn(lifecycleScope)
        uploadAdapter.uploadControlListener = object : OnItemClickListener<Upload, ItemListUploadBinding> {
            override fun onItemClick(position: Int, data: Upload, binding: ItemListUploadBinding) {
                requireUploadService().switchUpload(data)
            }
        }
        uploadAdapter.onItemClickListener = object : OnItemClickListener<Upload, ItemListUploadBinding> {
            override fun onItemClick(position: Int, data: Upload, binding: ItemListUploadBinding) {
                val popupMenu = PopupMenu(requireContext(), binding.root, Gravity.END)
                popupMenu.inflate(R.menu.menu_action_transmission)
                popupMenu.menu.enableMenuIcon()
                popupMenu.show()
                popupMenu.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.open_file -> {
                            data.path.getUploadFile(data.name).openFile()
                        }
                        R.id.share_file -> {
                            val shareIntent = data.path.getUploadFile(data.name).getShareIntent()
                            startActivity(shareIntent)
                        }
                        R.id.copy_text -> {
                            val url = "https://www.lanzoui.com/${data.encryptId}"
                            clipboardManager.setPrimaryClip(ClipData.newPlainText("url", url))
                        }
                        R.id.delete_download -> {
                            lifecycleScope.launch {
                                // 删除队列
                                if (requireUploadService().deleteUpload(data)) {
                                    uploadList.removeAt(position)
                                    uploadViewModel.remove(position)
                                    uploadAdapter.notifyItemRemoved(position)
                                }
                            }
                        }
                        R.id.detail_file -> {
                            val url = "https://www.lanzoui.com/${data.encryptId}"
                            MaterialAlertDialogBuilder(requireContext()).apply {
                                setTitle("文件详情")
                                val items = arrayOf(
                                    "文件名: ${data.name}",
                                    "文件大小: " + data.length.toSize(),
                                    "上传时间: " + data.time.handleTime(),
                                    "上传状态: " + data.getStatusStr(),
                                    "下载地址: $url",
                                    "当前路径: ${data.path}"
                                )
                                setItems(items) { _, index ->
                                    when (index) {
                                        0, 4, 5 -> {
                                            val item = items[index].split(" ")
                                            val content = item[1]
                                            clipboardManager.setPrimaryClip(
                                                ClipData.newPlainText(
                                                    "text",
                                                    content
                                                )
                                            )
                                            "${item[0]}已复制".showToast()
                                        }
                                    }
                                }
                                setPositiveButton("关闭", null)
                                show()
                            }
                        }
                    }
                    true
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUploadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (parentFragment is TransmissionFragment) {
            uploadAdapter.searchKeyWord = (parentFragment as TransmissionFragment).searchKey
        }

        binding.uploadRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = uploadAdapter
        }

        uploadViewModel.getUploadList().observe(viewLifecycleOwner) {
            uploadList.clear()
            uploadList.addAll(it)
            uploadAdapter.notifyData()
            uploadAdapter.update()
        }
    }

    override fun onResume() {
        super.onResume()
        val current = (parentFragment as TransmissionFragment).searchKey
        if (uploadAdapter.searchKeyWord != current) {
            onFilter(current)
        }
    }

    override fun onFilter(key: String?) {
        uploadAdapter.filter.filter(key)
    }

    override fun scrollToFirst() {
        binding.uploadRecyclerView.scrollToPosition(0)
    }

    override fun onUpload(uploadState: UploadState) {
        if (_binding == null) return
        if (binding.uploadRecyclerView.isComputingLayout) {
            return
        }
        when (uploadState) {
            is Insert -> {
                uploadList.add(0, uploadState.upload)
                uploadAdapter.notifyItemInserted(0)
            }
            else -> {
                val index = uploadList.indexOf(uploadState.upload)
                if (index != -1) {
                    // 这里进行更新
                    uploadAdapter.updateItem(index, uploadState.upload.status)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}