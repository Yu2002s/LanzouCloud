package cc.drny.lanzou.ui.upload

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.isInvisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import cc.drny.lanzou.R
import cc.drny.lanzou.adapter.FileSelectorAdapter
import cc.drny.lanzou.base.BaseSuperFragment
import cc.drny.lanzou.data.upload.FileInfo
import cc.drny.lanzou.databinding.FragmentUploadSelectorBinding
import cc.drny.lanzou.databinding.ItemListFileSelectorBinding
import cc.drny.lanzou.event.FileFilterable
import cc.drny.lanzou.event.OnItemClickListener
import cc.drny.lanzou.event.Scrollable
import cc.drny.lanzou.ui.TransmissionFragment
import com.google.android.material.card.MaterialCardView

class UploadSelectorFragment : BaseSuperFragment(), FileFilterable, Scrollable {

    private var _binding: FragmentUploadSelectorBinding? = null
    private val binding get() = _binding!!

    private val viewModel by navGraphViewModels<UploadSelectorViewModel>(R.id.uploadFileFragment)

    private val list = mutableListOf<FileInfo>()
    private val selectorAdapter = FileSelectorAdapter(list)

    private lateinit var navController: NavController

    private var type = 0

    companion object {

        fun newInstance(type: Int): UploadSelectorFragment {
            val args = Bundle()
            args.putInt("type", type)
            val fragment = UploadSelectorFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navController = findNavController()
        type = requireArguments().getInt("type", ClassificationFragment.TYPE_FILE)
        selectorAdapter.onItemClickListener = object : OnItemClickListener<FileInfo, ViewBinding> {
            override fun onItemClick(position: Int, data: FileInfo, binding: ViewBinding) {
                data.isSelected = !data.isSelected
                (binding.root as MaterialCardView).isChecked = data.isSelected
                if (data.isSelected) {
                    // add
                    Log.d("jdy", "add")
                    viewModel.addSelect(data)
                } else {
                    // remove
                    Log.d("jdy", "remove")
                    viewModel.removeSelect(data)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUploadSelectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.selectorRecyclerView.apply {
            val lm = GridLayoutManager(requireContext(), 2)
            layoutManager = lm
            if (type != ClassificationFragment.TYPE_ZIP) {
                selectorAdapter.enableAutoLoad().scopeIn(lifecycleScope)
            }
            adapter = selectorAdapter
        }

        lifecycleScope.launchWhenResumed {
            val navController = findNavController()
            val stateHandle = navController.currentBackStackEntry?.savedStateHandle
            stateHandle?.getLiveData<Boolean>("isUpload")
                ?.observe(viewLifecycleOwner) { isUpload ->
                    stateHandle.remove<Boolean>("isUpload")
                    if (isUpload) {
                        viewModel.selectedList.forEach { fileInfo ->
                            val index = list.indexOf(fileInfo)
                            selectorAdapter.updateItem(index, 0)
                        }
                        viewModel.selectedList.clear()
                    }
                }
            when (type) {
                ClassificationFragment.TYPE_FILE -> viewModel.getFiles()
                ClassificationFragment.TYPE_APP -> viewModel.getApps()
                ClassificationFragment.TYPE_IMG -> viewModel.getImages()
                ClassificationFragment.TYPE_ZIP -> viewModel.getZips(requireContext())
                ClassificationFragment.TYPE_APK -> viewModel.getApks()
                ClassificationFragment.TYPE_VIDEO -> viewModel.getVideos()
                ClassificationFragment.TYPE_DOCUMENT -> viewModel.getDocument(requireContext())

                else -> viewModel.getApps()
            }.observe(viewLifecycleOwner) {
                binding.progressbar.isInvisible = true
                list.clear()
                list.addAll(it)
                selectorAdapter.notifyData()
                if (type != ClassificationFragment.TYPE_ZIP) {
                    selectorAdapter.update()
                }
            }
        }

        navController.addOnDestinationChangedListener(destinationChangedListener)
    }

    override fun onFilter(key: String?) {
        selectorAdapter.filter.filter(key)
    }

    override fun scrollToFirst() {
        binding.selectorRecyclerView.scrollToPosition(0)
    }

    /**
     * 当从上传列表返回时，这里将被调用，检查文件是否是已选择的，如未选择，更新状态并删除已选择数据
     */
    private val destinationChangedListener = NavController.OnDestinationChangedListener { _, b, _ ->
        // changed
        if (b.id == R.id.uploadFileFragment) {
            val iterator = viewModel.selectedList.iterator()
            while (iterator.hasNext()) {
                val fileInfo = iterator.next()
                if (!fileInfo.isSelected && fileInfo.type == type) {
                    val index = list.indexOf(fileInfo)
                    selectorAdapter.updateItem(index, fileInfo, 0)
                    iterator.remove()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        navController.removeOnDestinationChangedListener(destinationChangedListener)
    }

}