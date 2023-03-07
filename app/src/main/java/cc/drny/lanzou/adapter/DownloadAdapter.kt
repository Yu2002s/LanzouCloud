package cc.drny.lanzou.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import cc.drny.lanzou.data.download.Download
import cc.drny.lanzou.data.upload.Upload
import cc.drny.lanzou.databinding.ItemListDownloadBinding
import cc.drny.lanzou.event.OnItemClickListener
import cc.drny.lanzou.util.FileUtils.toSize
import cc.drny.lanzou.util.getIcon
import cc.drny.lanzou.util.getIconForExtension

class DownloadAdapter(private val list: List<Download>) : BaseAdapter<Download, ItemListDownloadBinding>(list) {

    var downloadControlListener: OnItemClickListener<Download, ItemListDownloadBinding>? = null

    override fun onCreateBinding(parent: ViewGroup, viewType: Int): ItemListDownloadBinding {
        return ItemListDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onViewHolderCreated(holder: ViewHolder<ItemListDownloadBinding>) {
        holder.viewBinding.btnControlDownload.setOnClickListener {
            it.isSelected = !it.isSelected
            downloadControlListener!!.onItemClick(list[holder.adapterPosition], it)
        }
        holder.viewBinding.cbDownload.setOnClickListener {
            val data = list[holder.adapterPosition]
            data.isSelected = !data.isSelected
        }
    }

    override fun onBindView(data: Download, binding: ItemListDownloadBinding) {
        binding.tvFileName.text = data.name
        binding.tvFileSize.text = data.length.toSize()
        onBindView(data, binding, NOTIFY_KEY)
    }

    override fun onBindView(data: Download, binding: ItemListDownloadBinding, type: Any) {
        if (type == NOTIFY_KEY) {
            binding.cbDownload.isChecked = data.isSelected
        }
        binding.ivFileIcon.setImageDrawable(data.icon)
        if (binding.tvFileSize.text == "-1B") {
            binding.tvFileSize.text = data.length.toSize()
        }
        if (data.isCompleted() /*|| data.progress <= 0*/) {
            binding.progressHorizontal.isVisible = false
            binding.btnControlDownload.isVisible = false
        } else {
            binding.btnControlDownload.isVisible = true
            binding.btnControlDownload.isSelected = data.status == Upload.STATUS_PROGRESS
            binding.progressHorizontal.isVisible = true
            binding.progressHorizontal.progress = data.progress
        }

         binding.tvFileStatus.text = data.getStatusStr()
    }

    override fun isLoad(data: Download) = data.icon == null

    override fun getData(context: Context, data: Download): Any? {
        // 获取对应的图标
        if (data.isCompleted()) {
            data.icon = data.path.getIconForExtension(context, data.extension)
        } else {
            data.icon = data.extension?.getIcon(context)
        }
        return super.getData(context, data)
    }

    override fun onFilter(key: String, data: Download): Boolean {
        return data.name.lowercase().contains(key.lowercase())
    }

}