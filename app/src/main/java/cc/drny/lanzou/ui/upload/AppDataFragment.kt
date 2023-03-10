package cc.drny.lanzou.ui.upload

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewbinding.ViewBinding
import cc.drny.lanzou.adapter.FileSelectorAdapter
import cc.drny.lanzou.base.BaseSuperFragment
import cc.drny.lanzou.data.upload.FileInfo
import cc.drny.lanzou.databinding.FragmentAppDataBinding
import cc.drny.lanzou.event.FileFilterable
import cc.drny.lanzou.event.OnItemClickListener
import cc.drny.lanzou.event.Scrollable
import cc.drny.lanzou.ui.file.FileFragmentDirections

class AppDataFragment: BaseSuperFragment(), FileFilterable, Scrollable {

    private var _binding: FragmentAppDataBinding? = null
    private val binding get() = _binding!!
    private val files = mutableListOf<FileInfo>()
    private val fileAdapter = FileSelectorAdapter(files)

    private val viewModel by viewModels<AppDataViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // fileAdapter.enableAutoLoad().scopeIn(lifecycleScope)
        fileAdapter.onItemClickListener = object : OnItemClickListener<FileInfo, ViewBinding> {
            override fun onItemClick(position: Int, data: FileInfo, binding: ViewBinding) {
                findNavController()
                    .navigate(FileFragmentDirections
                        .actionGlobalAndroidDataFragment(data.path, data.name))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fileRecyclerView.apply {
            val gridLayoutManager = GridLayoutManager(requireContext(), 2)
            layoutManager = gridLayoutManager
            adapter = fileAdapter
        }

        viewModel.getApps(requireContext()).observe(viewLifecycleOwner) {
            binding.progressbar.isInvisible = true
            files.clear()
            files.addAll(it)
            fileAdapter.notifyDataSetChanged()
            binding.fileRecyclerView.scheduleLayoutAnimation()
        }
    }

    override fun scrollToFirst() {
        binding.fileRecyclerView.scrollToPosition(0)
    }

    override fun onFilter(key: String?) {
        fileAdapter.filter.filter(key)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}