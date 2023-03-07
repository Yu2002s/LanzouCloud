package cc.drny.lanzou.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import cc.drny.lanzou.data.lanzou.LanzouFile
import cc.drny.lanzou.databinding.ItemListFileBinding
import cc.drny.lanzou.databinding.ItemListFolderBinding
import com.google.android.material.card.MaterialCardView

class FileAdapter(files: List<LanzouFile>) : BaseAdapter<LanzouFile, ViewBinding>(files) {

    companion object {
        private const val TYPE_FILE = 0
        private const val TYPE_FOLDER = 1
    }

    override fun onCreateBinding(parent: ViewGroup, viewType: Int): ViewBinding {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_FILE -> ItemListFileBinding.inflate(inflater, parent, false)
            TYPE_FOLDER -> ItemListFolderBinding.inflate(inflater, parent, false)
            else -> throw IllegalStateException()
        }
    }

    override fun onBindView(data: LanzouFile, binding: ViewBinding) {
        onBindView(data, binding, NOTIFY_KEY)
        when (binding) {
            is ItemListFileBinding -> {
                binding.ivFileIcon.setImageDrawable(data.iconDrawable)
                binding.tvFileName.text = data.name_all
                binding.tvFileDesc.text =
                    String.format("%s - %s - %s下载", data.time, data.size, data.downloadCount)
            }

            is ItemListFolderBinding -> {
                binding.tvFileName.text = data.name
                binding.tvFileDesc.isVisible = !data.describe.isNullOrEmpty()
                binding.tvFileDesc.text = data.describe
            }
        }
    }

    override fun onBindView(data: LanzouFile, binding: ViewBinding, type: Any) {
        (binding.root as MaterialCardView).isChecked = data.isSelected
    }

    override fun getViewType(data: LanzouFile): Int {
        return if (data.icon == null) TYPE_FOLDER else TYPE_FILE
    }

    override fun onFilter(key: String, data: LanzouFile): Boolean {
        return data.getFileName().lowercase().contains(key.lowercase())
    }


}