package cc.drny.lanzou.ui.file

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.util.Log
import android.view.*
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import cc.drny.lanzou.R
import cc.drny.lanzou.adapter.BaseAdapter
import cc.drny.lanzou.adapter.FileAdapter
import cc.drny.lanzou.data.lanzou.LanzouFile
import cc.drny.lanzou.data.lanzou.LanzouPage
import cc.drny.lanzou.network.LanzouRepository
import cc.drny.lanzou.service.DownloadService
import cc.drny.lanzou.ui.download.DownloadViewModel
import cc.drny.lanzou.util.enableMenuIcon
import cc.drny.lanzou.util.getTextIntent
import cc.drny.lanzou.util.showToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Connection.Base

object FileActionHelper {

    fun initMenu(
        fragment: Fragment,
        menu: MenuItem,
        lanzouPage: LanzouPage,
        clipboardManager: ClipboardManager,
        searchView: SearchView?
    ): Boolean {

        with(fragment) {
            return when (menu.itemId) {
                R.id.delete_folder -> {
                    deleteFolder(lanzouPage)
                    true
                }

                R.id.new_folder -> {
                    searchView?.setOnQueryTextListener(null)
                    newFolder(lanzouPage)
                    true
                }

                R.id.copy_text -> {
                    shareFolder(lanzouPage, clipboardManager)
                    true
                }
                R.id.share_text -> {
                    shareFolder(lanzouPage)
                    true
                }

                R.id.edit_folder -> {
                    searchView?.setOnQueryTextListener(null)
                    editFolder(lanzouPage)
                    true
                }

                else -> false
            }
        }
    }

    fun createSearchMenuItem(
        searchView: SearchView?,
        fileAdapter: FileAdapter,
        viewModel: FileViewModel
    ) {
        searchView?.queryHint = "请输入关键字进行搜索"
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                if (newText == viewModel.key) return true
                viewModel.key = newText
                fileAdapter.filter(newText)
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }
        })
    }

    fun showFileActionPopupMenu(
        fragment: Fragment,
        downloadService: DownloadService,
        downloadViewModel: DownloadViewModel,
        fileAdapter: FileAdapter,
        lanzouFiles: MutableList<LanzouFile>,
        v: View,
        lanzouFile: LanzouFile,
        position: Int,
        lanzouPage: LanzouPage,
        clipboardManager: ClipboardManager,
        searchView: SearchView?,
        callback: () -> Unit
    ) {
        with(fragment) {
            val popupMenu = PopupMenu(requireContext(), v, Gravity.END)
            popupMenu.inflate(R.menu.menu_file_action)
            popupMenu.menu.enableMenuIcon()
            popupMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.download -> {
                        if (lanzouFile.isSelected) {
                            downloadFiles(
                                lanzouFiles,
                                fileAdapter,
                                downloadService,
                                downloadViewModel,
                                callback
                            )
                        } else {
                            downloadService.addDownload(lanzouFile) { download ->
                                downloadViewModel.addDownload(download)
                            }
                        }
                    }

                    R.id.delete -> {
                        if (lanzouFile.isSelected) {
                            deleteFiles(lanzouFiles, fileAdapter, callback)
                        } else {
                            deleteFile(lanzouFile, position, lanzouFiles, fileAdapter)
                        }
                    }

                    R.id.copy_text -> {
                        if (lanzouFile.isSelected) {
                            callback.invoke()
                            shareFiles(lanzouFiles, fileAdapter, clipboardManager)
                        } else {
                            shareFile(lanzouFile, clipboardManager)
                        }
                    }
                    R.id.share_text -> {
                        if (lanzouFile.isSelected) {
                            callback.invoke()
                            shareFiles(lanzouFiles, fileAdapter)
                        } else {
                            shareFile(lanzouFile)
                        }
                    }
                    R.id.share_file -> {

                    }

                    R.id.move -> {
                        searchView?.setOnQueryTextListener(null)
                        if (lanzouFile.isSelected) {
                            moveFiles(lanzouFiles, lanzouPage, fileAdapter)
                        } else {
                            moveFile(lanzouFile, lanzouPage)
                        }
                    }

                    R.id.detail -> {
                        searchView?.setOnQueryTextListener(null)
                        editFile(lanzouFile)
                    }
                }
                true
            }
            popupMenu.show()
        }
    }

    private fun Fragment.downloadFiles(
        lanzouFiles: List<LanzouFile>,
        fileAdapter: FileAdapter,
        downloadService: DownloadService,
        downloadViewModel: DownloadViewModel,
        callback: () -> Unit
    ) {
        // 多文件下载
        var count = lanzouFiles.count { it.isSelected }
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle("下载" + count + "个文件")
            setMessage("是否立即下载已选择文件")
            setPositiveButton("下载") { _, _ ->
                callback.invoke()
                lanzouFiles.forEachIndexed { index, lanzouFile ->
                    if (lanzouFile.isSelected) {
                        lanzouFile.isSelected = false
                        fileAdapter.updateItems(index, lanzouFile, BaseAdapter.NOTIFY_KEY)
                        if (lanzouFile.isFile()) {
                            downloadService.addDownload(lanzouFile) { download ->
                                downloadViewModel.addDownload(download)
                            }
                        }
                        if (--count == 0) return@forEachIndexed
                    }
                }
            }
            setNegativeButton("取消", null)
            show()
        }
    }

    private fun Fragment.deleteFile(
        lanzouFile: LanzouFile,
        position: Int,
        lanzouFiles: MutableList<LanzouFile>,
        fileAdapter: FileAdapter
    ) {
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle("删除${lanzouFile.getFileName()}")
            setMessage("是否删除该文件，删除后可在回收站中恢复")
            setPositiveButton("删除") { _, _ ->
                // 删除文件
                lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        LanzouRepository.deleteFileOrFolder(lanzouFile.getId(), lanzouFile.isFile())
                    }
                    result.onSuccess { msg ->
                        msg.showToast()
                        lanzouFiles.removeAt(position)
                        fileAdapter.notifyItemRemoved(position)
                    }.onFailure { throwable ->
                        throwable.message.showToast()
                    }
                }
            }
            setNegativeButton("取消", null)
            show()
        }
    }

    private fun Fragment.deleteFiles(
        lanzouFiles: MutableList<LanzouFile>,
        fileAdapter: FileAdapter,
        callback: () -> Unit
    ) {
        var count = lanzouFiles.count { it.isSelected }
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle("删除" + count + "个文件")
            setMessage("是否立即删除已选择文件,可在回收站中恢复")
            setPositiveButton("删除") { _, _ ->
                callback.invoke()
                lifecycleScope.launch {
                    for (i in lanzouFiles.size - 1 downTo 0) {
                        val lanzouFile = lanzouFiles[i]
                        if (lanzouFile.isSelected) {
                            withContext(Dispatchers.IO) {
                                LanzouRepository.deleteFileOrFolder(
                                    lanzouFile.getId(),
                                    lanzouFile.isFile()
                                )
                            }.onSuccess {
                                fileAdapter.removeItems(i, lanzouFile)
                            }
                            if (--count == 0) {
                                break
                            }
                        }
                    }
                }
            }
            setNegativeButton("取消", null)
            show()
        }
    }

    private fun Fragment.shareFiles(
        lanzouFiles: List<LanzouFile>,
        fileAdapter: FileAdapter,
        clipboardManager: ClipboardManager? = null
    ) {
        lifecycleScope.launch {
            var count = 0
            val content = with(StringBuilder()) {
                for (index in lanzouFiles.indices) {
                    val lanzouFile = lanzouFiles[index]
                    if (!lanzouFile.isSelected) {
                        continue
                    }
                    count++
                    val result = withContext(Dispatchers.IO) {
                        if (lanzouFile.isFile()) {
                            LanzouRepository.getFileInfo(lanzouFile.fileId)
                        } else {
                            LanzouRepository.getFolder(lanzouFile.folderId)
                        }
                    }
                    result.onSuccess {
                        lanzouFile.isSelected = false
                        fileAdapter.updateItems(index, lanzouFile, BaseAdapter.NOTIFY_KEY)
                        append(lanzouFile.getFileName())
                        append("\n")
                        if (lanzouFile.isFile()) {
                            val content = LanzouRepository.getShareUrl(it)
                            append(content)
                        } else {
                            append(it.url)
                        }
                        if (it.hasPwd == 1) {
                            append("\n")
                            append(it.pwd)
                        }
                        if (index != lanzouFiles.size - 1) {
                            append("\n")
                        }
                    }
                }
                toString()
            }
            if (clipboardManager != null) {
                clipboardManager.setPrimaryClip(ClipData.newPlainText("urls", content))
                "${count}个文件分享地址已复制".showToast()
            } else {
                val intent = content.getTextIntent()
                startActivity(Intent.createChooser(intent, "分享到"))
            }
        }
    }

    private fun Fragment.shareFile(
        lanzouFile: LanzouFile,
        clipboardManager: ClipboardManager? = null
    ) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                LanzouRepository.getFileInfo(lanzouFile.fileId)
            }
            result.onSuccess {
                var url = LanzouRepository.getShareUrl(it)
                if (it.hasPwd == 1) {
                    url += "\n密码: " + it.pwd
                }
                if (clipboardManager != null) {
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("url", url))
                    "分享地址已复制".showToast()
                } else {
                    val intent = url.getTextIntent()
                    startActivity(Intent.createChooser(intent, "分享到"))
                }
            }
        }
    }

    private fun Fragment.editFile(lanzouFile: LanzouFile) {
        findNavController().navigate(
            FileFragmentDirections
                .actionFileFragmentToFileDetailsDialogFragment(
                    lanzouFile.fileId,
                    lanzouFile.name_all
                )
        )
    }

    private fun Fragment.moveFile(lanzouFile: LanzouFile, lanzouPage: LanzouPage) {
        if (lanzouFile.isFile()) {
            findNavController().navigate(
                FileFragmentDirections
                    .actionFileFragmentToMoveFileDialogFragment(
                        lanzouPage,
                        longArrayOf(lanzouFile.fileId)
                    )
            )
        }
    }

    private fun Fragment.moveFiles(
        lanzouFiles: MutableList<LanzouFile>,
        lanzouPage: LanzouPage,
        fileAdapter: FileAdapter
    ) {
        val ids = lanzouFiles.filterIndexed { index, file ->
            if (file.isSelected && file.isFolder()) {
                file.isSelected = false
                fileAdapter.updateItem(index, BaseAdapter.NOTIFY_KEY)
            }
            file.isSelected
        }
            .map { file -> file.fileId }
            .toLongArray()
        findNavController().navigate(
            FileFragmentDirections
                .actionFileFragmentToMoveFileDialogFragment(
                    lanzouPage,
                    ids
                )
        )
    }

    private fun Fragment.newFolder(lanzouPage: LanzouPage) {
        findNavController().navigate(
            FileFragmentDirections
                .actionFileFragmentToNewFolderDialogFragment(lanzouPage.folderId)
        )
    }

    private fun Fragment.shareFolder(lanzouPage: LanzouPage, clipboardManager: ClipboardManager? = null) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                LanzouRepository.getFolder(lanzouPage.folderId)
            }
            result.onSuccess {
                var url = it.url
                if (it.hasPwd == 1) {
                    url += "\n密码: " + it.pwd
                }
                if (clipboardManager != null) {
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("url", url))
                    "分享地址已复制".showToast()
                } else {
                    val intent = url.getTextIntent()
                    startActivity(Intent.createChooser(intent, "分享到"))
                }
            }.onFailure {
                it.message.showToast()
            }
        }
    }

    private fun Fragment.editFolder(lanzouPage: LanzouPage) {
        findNavController().navigate(
            FileFragmentDirections.actionFileFragmentToFileDetailsDialogFragment(
                lanzouPage.folderId,
                lanzouPage.name,
                false
            )
        )
    }

    /**
     * 删除文件夹
     */
    private fun Fragment.deleteFolder(lanzouPage: LanzouPage) {
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle("删除${lanzouPage.name}")
            setMessage("当文件夹含有子文件无法进行删除，删除后可在回收站中恢复")
            setNegativeButton("取消", null)
            setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        LanzouRepository.deleteFileOrFolder(lanzouPage.folderId, false)
                    }
                    result.onSuccess {
                        it.showToast()
                        val navController = findNavController()
                        navController.previousBackStackEntry
                            ?.savedStateHandle?.set("deleteFolder", lanzouPage.folderId)
                        navController.popBackStack()
                    }.onFailure {
                        Log.e("jdy", it.message.toString())
                        it.message.showToast()
                    }
                }
            }
            show()
        }
    }

}