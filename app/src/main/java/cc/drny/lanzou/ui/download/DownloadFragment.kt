package cc.drny.lanzou.ui.download

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import cc.drny.lanzou.R
import cc.drny.lanzou.adapter.BaseAdapter
import cc.drny.lanzou.adapter.DownloadAdapter
import cc.drny.lanzou.adapter.FileAdapter
import cc.drny.lanzou.base.BaseDownloadFragment
import cc.drny.lanzou.data.download.Download
import cc.drny.lanzou.databinding.FragmentDownloadBinding
import cc.drny.lanzou.databinding.ItemListDownloadBinding
import cc.drny.lanzou.event.FileFilterable
import cc.drny.lanzou.event.OnItemClickListener
import cc.drny.lanzou.event.Scrollable
import cc.drny.lanzou.util.DateUtils.handleTime
import cc.drny.lanzou.util.FileUtils.toSize
import cc.drny.lanzou.util.enableMenuIcon
import cc.drny.lanzou.util.getIntent
import cc.drny.lanzou.util.openFile
import cc.drny.lanzou.util.showToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File

class DownloadFragment : BaseDownloadFragment(), FileFilterable, Scrollable {

    private var _binding: FragmentDownloadBinding? = null
    private val binding get() = _binding!!

    private val downloadViewModel by navGraphViewModels<DownloadViewModel>(R.id.fileFragment)
    private val downloadList = mutableListOf<Download>()
    private val downloadAdapter = DownloadAdapter(downloadList)

    private val clipboardManager by lazy {
        requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        downloadAdapter.enableAutoLoad().scopeIn(lifecycleScope)
        downloadAdapter.downloadControlListener =
            object : OnItemClickListener<Download, ItemListDownloadBinding> {
                override fun onItemClick(data: Download, v: View) {
                    requireDownloadService().switchDownload(data)
                }
            }
        downloadAdapter.onItemClickListener =
            object : OnItemClickListener<Download, ItemListDownloadBinding> {
                override fun onItemClick(position: Int, v: View) {
                    val data = downloadList[position]
                    showPopupMenu(v, data, position)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.downloadRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = downloadAdapter
        }

        lifecycleScope.launch {
            downloadViewModel.getDownloadList().observe(viewLifecycleOwner) { downloads ->
                downloadList.clear()
                downloadList.addAll(downloads)
                downloadAdapter.notifyData()
                downloadAdapter.update()
            }
        }
    }

    override fun onFilter(key: String?) {
        downloadAdapter.filter.filter(key)
    }

    override fun scrollToFirst() {
        binding.downloadRecyclerView.scrollToPosition(0)
    }

    private fun showPopupMenu(view: View, data: Download, position: Int) {
        val popupMenu = PopupMenu(requireContext(), view, Gravity.END)
        popupMenu.inflate(R.menu.menu_action_transmission)
        popupMenu.menu.enableMenuIcon()
        popupMenu.show()
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.open_file -> {
                    if (data.isCompleted()) {
                        if (!data.path.openFile()) {
                            "源文件不存在".showToast()
                        }
                    } else "请等待下载完成".showToast()
                }
                R.id.share_file -> {
                    if (data.isSelected) {
                        /*val files = mutableListOf<File>()
                        downloadList.forEachIndexed { index, download ->
                            if (download.isSelected && download.isCompleted()) {
                                download.isSelected = false
                                val p = downloadAdapter.getPosition(index, download)
                                downloadAdapter.updateItem(p, BaseAdapter.NOTIFY_KEY)
                                // val d = downloadAdapter.getFilterData()[p]
                                files.add(File(download.path))
                            }
                        }
                        if (files.isNotEmpty()) {
                            requireContext().startActivity(
                                Intent.createChooser(
                                    files.getIntent(),
                                    "分享${files.size}个文件到"
                                )
                            )
                        }*/
                    } else {
                        if (data.isCompleted()) {
                            val file = File(data.path)
                            val share = file.getIntent(Intent.ACTION_SEND)
                            requireContext().startActivity(Intent.createChooser(share, "分享到"))
                        }
                    }
                }
                R.id.delete_download -> {
                    if (data.isSelected) {
                        lifecycleScope.launch {
                            deleteFiles()
                        }
                    } else {
                        MaterialAlertDialogBuilder(requireContext()).apply {
                            setTitle("删除${data.name}")
                            setMessage("此操作将会删除本地文件以及下载记录")
                            setPositiveButton("删除") { _, _ ->
                                lifecycleScope.launch {
                                    downloadService?.deleteDownload(data) {
                                        downloadViewModel.removeDownload(position)
                                        downloadList.removeAt(position)
                                        downloadAdapter.notifyItemRemoved(position)
                                    }
                                }
                            }
                            setNegativeButton("取消", null)
                            show()
                        }
                    }
                }
                R.id.detail_file -> {
                    MaterialAlertDialogBuilder(requireContext()).apply {
                        setTitle("文件详情")
                        val items = arrayOf(
                            "文件名: ${data.name}",
                            "文件大小: " + data.length.toSize(),
                            "下载时间: " + data.time.handleTime(),
                            "下载状态: " + data.getStatusStr(),
                            "下载地址: " + data.url,
                            "下载密码: " + (data.pwd ?: "未设置"),
                            "当前路径: ${data.path}"
                        )
                        setItems(items) { _, index ->
                            when (index) {
                                0, 4, 5, 6 -> {
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

    private fun deleteFiles() {
        var count = downloadList.count { it.isSelected }
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle("删除" + count + "个文件")
            setMessage("此操作将会删除本地文件以及下载记录")
            setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    for (i in downloadList.size - 1 downTo 0) {
                        val download = downloadList[i]
                        if (download.isSelected) {
                            downloadService?.deleteDownload(download) {
                                downloadViewModel.removeDownload(i)
                                downloadList.removeAt(i)
                                downloadAdapter.notifyItemRemoved(i)
                            }
                            if (--count == 0) break
                        }
                    }
                }
            }
            setNegativeButton("取消", null)
            show()
        }
    }

    override fun onDownload(download: Download, error: String?) {
        if (download.isInsert()) {
            downloadList.add(0, download)
            downloadAdapter.notifyItemInserted(0)
        } else {
            val index = downloadList.indexOf(download)
            if (index == -1) return
            downloadAdapter.updateItem(index, download.status)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}