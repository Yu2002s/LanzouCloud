package cc.drny.lanzou.ui.user

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import cc.drny.lanzou.LanzouApplication
import cc.drny.lanzou.R
import cc.drny.lanzou.adapter.BaseAdapter
import cc.drny.lanzou.base.BaseFragment
import cc.drny.lanzou.data.lanzou.LanzouShareFile
import cc.drny.lanzou.databinding.FragmentUserBinding
import cc.drny.lanzou.databinding.ItemListGridBinding
import cc.drny.lanzou.event.OnItemClickListener
import cc.drny.lanzou.network.LanzouRepository
import cc.drny.lanzou.util.showToast
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class UserFragment : BaseFragment() {

    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<UserViewModel>()

    private val list =
        mutableListOf(
            mapOf("icon" to R.drawable.baseline_delete_24, "title" to "回收站"),
            mapOf("icon" to R.drawable.baseline_link_24, "title" to "解析文件"),
            mapOf("icon" to R.drawable.baseline_qr_code_scanner_24, "title" to "扫一扫")
        )

    private val adapter = MyAdapter(list.toMutableList())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val register = registerForActivityResult(ScanContract()) {
            val contents = it.contents
            if (contents.isNullOrEmpty()) return@registerForActivityResult
            if (!contents.startsWith("https://")) {
                Toast.makeText(requireContext(), "非分享地址", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            findNavController().navigate(
                UserFragmentDirections
                    .actionGlobalAnalyzeFileDialogFragment(
                        arrayOf(LanzouShareFile(contents))
                    )
            )
        }

        adapter.onItemClickListener =
            object : OnItemClickListener<Map<String, Any>, ItemListGridBinding> {
                override fun onItemClick(
                    position: Int,
                    data: Map<String, Any>,
                    binding: ItemListGridBinding
                ) {
                    when (position) {
                        0 -> {
                            findNavController().navigate(
                                UserFragmentDirections.actionUserFragmentToRecycleBinFragment()
                            )
                        }
                        1 -> {
                            findNavController().navigate(
                                UserFragmentDirections.actionGlobalAnalyzeFileDialogFragment()
                            )
                        }
                        2 -> {
                            val scanOptions = ScanOptions()
                            scanOptions.apply {
                                setOrientationLocked(false)
                                setBeepEnabled(false)
                                setBarcodeImageEnabled(true)
                            }
                            register.launch(scanOptions)
                        }
                    }
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
        savedStateHandle?.getLiveData<Boolean>("isLogin")
            ?.observe(viewLifecycleOwner) { isLogin ->
                savedStateHandle.remove<Boolean>("isLogin")
                if (isLogin) {
                    viewModel.getUserData()
                }
            }

        binding.btnGoLogin.setOnClickListener {
            if (LanzouRepository.getUserCookie() != null) {
                viewModel.logout()
                LanzouRepository.logout()
            }
            navController.navigate(R.id.action_userFragment_to_loginFragment)
        }

        viewModel.userLiveData.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                binding.btnGoLogin.text = "退出登录"
                binding.tvUsername.text = it
            }.onFailure {
                binding.btnGoLogin.text = "去登录"
                binding.tvUsername.text = it.message
            }
        }

        binding.actionRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.actionRecyclerView.adapter = adapter

    }

    private class MyAdapter(list: MutableList<Map<String, Any>>) :
        BaseAdapter<Map<String, Any>, ItemListGridBinding>(list) {
        override fun onCreateBinding(parent: ViewGroup, viewType: Int): ItemListGridBinding {
            return ItemListGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        }

        override fun onBindView(data: Map<String, Any>, binding: ItemListGridBinding) {
            binding.icon.setImageResource(data["icon"] as Int)
            binding.tvTitle.text = data["title"].toString()
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}