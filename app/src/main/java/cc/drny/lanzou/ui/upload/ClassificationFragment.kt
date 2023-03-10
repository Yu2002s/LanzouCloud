package cc.drny.lanzou.ui.upload

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import cc.drny.lanzou.databinding.FragmentClassifcationBinding
import cc.drny.lanzou.event.FileFilterable
import cc.drny.lanzou.event.Scrollable
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayoutMediator

class ClassificationFragment : Fragment(), FileFilterable {

    private var _binding: FragmentClassifcationBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TYPE_FILE = 0
        const val TYPE_APP = 1
        const val TYPE_IMG = 2
        const val TYPE_ZIP = 3
        const val TYPE_APK = 4
        const val TYPE_VIDEO = 5
        const val TYPE_DOCUMENT = 6
    }

    private val types =
        arrayOf(TYPE_FILE, TYPE_APP, TYPE_IMG, TYPE_ZIP, TYPE_APK, TYPE_VIDEO, TYPE_DOCUMENT)

    private val titles = arrayOf("最近", "软件", "图片", "压缩包", "安装包", "视频", "文档")

    private val fragments = types.map { UploadSelectorFragment.newInstance(it) } as MutableList

    private lateinit var adapter: MyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            childFragmentManager.fragments.forEach { old ->
                val oldType = old.requireArguments().getInt("type")
                fragments.forEachIndexed { index, new ->
                    val newType = new.requireArguments().getInt("type")
                    if (newType == oldType) {
                        fragments[index] = old as UploadSelectorFragment
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClassifcationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MyAdapter()
        binding.vpClassification.adapter = adapter

        TabLayoutMediator(binding.tabLayoutClassification, binding.vpClassification) { tab, position ->
            tab.text = titles[position]
        }.attach()

        binding.tabLayoutClassification.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {
                fragments[tab.position].scrollToFirst()
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }
        })
    }

    override fun onFilter(key: String?) {
        if (_binding == null) return
        val fragment = fragments[binding.vpClassification.currentItem]
        fragment.onFilter(key)
    }

    private inner class MyAdapter :
        FragmentStateAdapter(childFragmentManager, lifecycle) {

        override fun getItemCount() = fragments.size

        override fun createFragment(position: Int) = fragments[position]

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}