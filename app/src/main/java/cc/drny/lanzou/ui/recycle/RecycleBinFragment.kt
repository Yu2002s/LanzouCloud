package cc.drny.lanzou.ui.recycle

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.activity.addCallback
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.view.MenuProvider
import androidx.core.view.isInvisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewbinding.ViewBinding
import cc.drny.lanzou.R
import cc.drny.lanzou.adapter.BaseAdapter
import cc.drny.lanzou.adapter.FileAdapter
import cc.drny.lanzou.base.BaseSuperFragment
import cc.drny.lanzou.data.lanzou.LanzouFile
import cc.drny.lanzou.databinding.FragmentRecycleBinBinding
import cc.drny.lanzou.event.OnItemClickListener
import cc.drny.lanzou.event.OnItemLongClickListener
import cc.drny.lanzou.network.LanzouRepository
import cc.drny.lanzou.util.enableMenuIcon
import cc.drny.lanzou.util.showToast
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecycleBinFragment : BaseSuperFragment(), MenuProvider {

    private var _binding: FragmentRecycleBinBinding? = null
    private val binding get() = _binding!!

    private val lanzouFiles = mutableListOf<LanzouFile>()
    private val fileAdapter = FileAdapter(lanzouFiles)

    private val viewModel by viewModels<RecycleBinViewModel>()

    private var isMultiMode
        get() = viewModel.isMultiMode
        set(value) {
            viewModel.isMultiMode = value
        }

    private var selectedCount
        get() = viewModel.selectedCount
        set(value) {
            viewModel.selectedCount = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileAdapter.onItemClickListener = object : OnItemClickListener<LanzouFile, ViewBinding> {
            override fun onItemClick(position: Int, v: View) {
                val lanzouFile = lanzouFiles[position]
                if (isMultiMode) {
                    lanzouFile.isSelected = !lanzouFile.isSelected
                    (v as MaterialCardView).isChecked = lanzouFile.isSelected
                    if (lanzouFile.isSelected) {
                        selectedCount ++
                    } else {
                        selectedCount --
                    }
                    if (selectedCount == 0) {
                        isMultiMode = false
                    }
                    return
                }
                showPopupMenu(v, lanzouFile, position)
            }
        }
        fileAdapter.onItemLongClickListener =
            object : OnItemLongClickListener<LanzouFile, ViewBinding> {
                override fun onItemLongClick(position: Int, v: View) {
                    val lanzouFile = lanzouFiles[position]
                    if (lanzouFile.isSelected) {
                        showPopupMenu(v, lanzouFile, position)
                    } else {
                        selectedCount ++
                        lanzouFile.isSelected = true
                        (v as MaterialCardView).isChecked = true
                        if (!isMultiMode) {
                            isMultiMode = true
                        }
                    }
                }
            }
    }

    override fun onCreateMenu(p0: Menu, p1: MenuInflater) {
        p0.enableMenuIcon()
        p1.inflate(R.menu.menu_recycle_action, p0)

        val searchView = p0.findItem(R.id.search_file).actionView as SearchView
        searchView.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                fileAdapter.filter.filter(newText)
                return false
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onMenuItemSelected(p0: MenuItem): Boolean {
        if (p0.itemId == android.R.id.home || p0.itemId == R.id.search_file) {
            return false
        }
        val isRestore = p0.itemId == R.id.restore_file
        val str = if (isRestore) "恢复" else "清空"
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(str + "回收站")
            setMessage("是否对回收站的所有文件进行$str")
            setPositiveButton("是") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        LanzouRepository.restoreOrDeleteRecycleBin(isRestore)
                    }.onSuccess {
                        it.showToast()
                        lanzouFiles.clear()
                        fileAdapter.notifyDataSetChanged()
                    }.onFailure {
                        it.message.showToast()
                    }
                }
            }
            setNegativeButton("否", null)
            show()
        }
        return true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
        _binding = FragmentRecycleBinBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fileRecyclerView.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter = fileAdapter
        }

        viewModel.recycleFiles(requireContext()).observe(viewLifecycleOwner) { result ->
            binding.progressbar.isInvisible = true
            result.onSuccess {
                lanzouFiles.clear()
                lanzouFiles.addAll(it)
                fileAdapter.notifyDataSetChanged()
                binding.fileRecyclerView.scheduleLayoutAnimation()
            }.onFailure {
                it.message.showToast()
            }
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

    private fun showPopupMenu(view: View, lanzouFile: LanzouFile, position: Int) {
        val popupMenu = PopupMenu(requireContext(), view, Gravity.END)
        popupMenu.inflate(R.menu.menu_recycle_action)
        popupMenu.menu.enableMenuIcon()
        popupMenu.show()
        popupMenu.setOnMenuItemClickListener {
            if (lanzouFile.isSelected) {
                restoreOrDeleteFiles(it.itemId == R.id.restore_file)
            } else {
                restoreOrDeleteFile(lanzouFile, position, it.itemId == R.id.restore_file)
            }
            true
        }
    }

    /**
     * 删除或者恢复单个文件
     */
    private fun restoreOrDeleteFile(
        lanzouFile: LanzouFile,
        position: Int,
        isRestore: Boolean = true
    ) {
        val str = if (isRestore) "恢复" else "删除"
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(str + lanzouFile.getFileName())
            setMessage("是否${str}该文件,文件${str}后将显示在根目录，请返回文件列表后进行刷新即可")
            setPositiveButton("是") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        LanzouRepository.restoreOrDeleteFile(
                            lanzouFile.getId(),
                            isRestore,
                            lanzouFile.isFolder()
                        )
                    }.onSuccess { result ->
                        result.showToast()
                        lanzouFiles.removeAt(position)
                        fileAdapter.notifyItemRemoved(position)
                    }.onFailure { throwable ->
                        throwable.message.showToast()
                    }
                }
            }
            setNegativeButton("否", null)
            show()
        }
    }

    /**
     * 删除或者恢复多个文件
     */
    private fun restoreOrDeleteFiles(isRestore: Boolean = true) {
        // 多选操作
        val str = if (isRestore) "恢复" else "删除"
        var count = lanzouFiles.count { it.isSelected }
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle("$str${count}个文件")
            setMessage("文件${str}后将显示在根目录,请返回文件列表后进行刷新即可")
            setPositiveButton("是") { _, _ ->
                isMultiMode = false
                lifecycleScope.launch {
                    for (i in lanzouFiles.size - 1 downTo 0) {
                        val lanzouFile = lanzouFiles[i]
                        if (lanzouFile.isSelected) {
                            withContext(Dispatchers.IO) {
                                LanzouRepository.restoreOrDeleteFile(
                                    lanzouFile.getId(),
                                    isRestore,
                                    lanzouFile.isFolder()
                                )
                            }.onSuccess {
                                lanzouFiles.removeAt(i)
                                fileAdapter.notifyItemRemoved(i)
                            }
                            if (--count == 0) {
                                break
                            }
                        }
                    }
                }
            }
            setNegativeButton("否", null)
            show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        requireActivity().removeMenuProvider(this)
    }
}