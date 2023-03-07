package cc.drny.lanzou.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import cc.drny.lanzou.data.upload.Upload
import cc.drny.lanzou.databinding.ItemListUploadBinding
import cc.drny.lanzou.event.OnItemClickListener
import cc.drny.lanzou.util.FileUtils.toSize
import cc.drny.lanzou.util.getIconForExtension

class UploadAdapter(private val uploadList: List<Upload>): BaseAdapter<Upload, ItemListUploadBinding>(uploadList) {

    var uploadControlListener: OnItemClickListener<Upload, ItemListUploadBinding>? = null

    override fun onCreateBinding(parent: ViewGroup, viewType: Int): ItemListUploadBinding {
        return ItemListUploadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onBindView(data: Upload, binding: ItemListUploadBinding) {
        binding.tvFileName.text = data.name
        binding.tvFileSize.text = data.length.toSize()
        binding.tvFolderName.text = data.folderName

        onBindView(data, binding, NOTIFY_KEY)
    }

    override fun onViewHolderCreated(holder: ViewHolder<ItemListUploadBinding>) {
        holder.viewBinding.btnControlUpload.setOnClickListener {
            it.isSelected = !it.isSelected
            uploadControlListener!!.onItemClick(uploadList[holder.adapterPosition], it)
        }
    }

    override fun getNotifyKey(data: Upload): Int {
        // 默认KEY用于更新ICON
        return if (data.icon == null) NOTIFY_KEY else data.status
    }

    override fun getData(context: Context, data: Upload): Any? {
        // 这里异步加载ICON
        data.icon = data.path.getIconForExtension(context, data.extension)
        return super.getData(context, data)
    }

    override fun isLoad(data: Upload) = data.icon == null

    override fun onBindView(data: Upload, binding: ItemListUploadBinding, type: Any) {
        if (type == NOTIFY_KEY) {
            // 默认用于更新Icon
            binding.ivFileIcon.setImageDrawable(data.icon)
        } else if (type == Upload.STATUS_COMPLETED) {
            binding.tvFolderName.text = data.folderName
        }
        if (data.isCompleted()) {
            binding.progressHorizontal.isVisible = false
            binding.btnControlUpload.isVisible = false
        } else {
            binding.btnControlUpload.isVisible = true
            binding.btnControlUpload.isSelected = data.status == Upload.STATUS_PROGRESS
            binding.progressHorizontal.isVisible = true
            binding.progressHorizontal.progress = data.progress
        }

        binding.tvFileStatus.text = data.getStatusStr()
    }

    override fun onFilter(key: String, data: Upload): Boolean {
        return data.name.lowercase().contains(key.lowercase())
    }

}