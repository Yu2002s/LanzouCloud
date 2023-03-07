package cc.drny.lanzou.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import cc.drny.lanzou.R
import cc.drny.lanzou.data.lanzou.LanzouShareFile
import cc.drny.lanzou.databinding.ItemListFileBinding

class ShareFileAdapter(files: List<LanzouShareFile>) :
    BaseAdapter<LanzouShareFile, ItemListFileBinding>(files) {

    override fun onCreateBinding(parent: ViewGroup, viewType: Int): ItemListFileBinding {
        return ItemListFileBinding.inflate(LayoutInflater.from(parent.context), parent, false).apply {
            // cbFile.isVisible = false
            (ivFileIcon.layoutParams as ViewGroup.MarginLayoutParams).marginStart = 0
        }
    }

    override fun onBindView(data: LanzouShareFile, binding: ItemListFileBinding) {
        binding.ivFileIcon.setImageDrawable(data.icon
            ?: ContextCompat.getDrawable(binding.root.context, R.drawable.baseline_link_24))
        binding.tvFileName.text = data.name ?: data.url
        binding.tvFileDesc.text = data.getShareDesc()
    }
}