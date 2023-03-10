package cc.drny.lanzou.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import cc.drny.lanzou.data.upload.FileInfo
import cc.drny.lanzou.databinding.ItemListFileSelectorBinding
import cc.drny.lanzou.databinding.ItemListFolderBinding
import cc.drny.lanzou.util.getIconForExtension

class FileSelectorAdapter(files: MutableList<FileInfo>) : BaseAdapter<FileInfo, ViewBinding>(files) {

    override fun onCreateBinding(parent: ViewGroup, viewType: Int): ViewBinding {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> ItemListFolderBinding.inflate(layoutInflater, parent, false)
            else -> ItemListFileSelectorBinding.inflate(
                layoutInflater,
                parent,
                false
            )
        }
    }

    override fun onBindView(data: FileInfo, binding: ViewBinding) {
        if (binding is ItemListFileSelectorBinding) {
            binding.tvFileName.text = data.name
            binding.tvFileDesc.text = data.fileDesc
            binding.root.isChecked = data.isSelected
           // binding.checkbox.isVisible = data.extension != null
           // binding.checkbox.isChecked = data.isSelected
        } else if (binding is ItemListFolderBinding) {
            binding.ivFileIcon.setImageDrawable(data.icon)
            binding.tvFileName.text = data.name
            binding.tvFileDesc.text = data.fileDesc
        }


        onBindView(data, binding, NOTIFY_KEY)
    }

    override fun onBindView(data: FileInfo, binding: ViewBinding, type: Any) {
        if (binding is ItemListFileSelectorBinding) {
            if (type == 0) {
                binding.root.isChecked = data.isSelected
            }
            binding.ivFileIcon.setImageDrawable(data.icon)
        }

    }

    override fun getData(context: Context, data: FileInfo): Any? {
        data.icon = data.path.getIconForExtension(context, data.extension)
        return super.getData(context, data)
    }

    override fun isLoad(data: FileInfo) = data.icon == null

    override fun getViewType(data: FileInfo): Int {
        return if (data.extension == null) 0 else 1
    }

    override fun onFilter(key: String, data: FileInfo): Boolean {
        return data.name.lowercase().contains(key.lowercase())
    }
}