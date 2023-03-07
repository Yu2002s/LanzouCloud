package cc.drny.lanzou.ui.upload

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import cc.drny.lanzou.base.BaseFragment
import cc.drny.lanzou.base.BaseSuperFragment
import cc.drny.lanzou.databinding.FragmentFileManagerBinding
import cc.drny.lanzou.event.FileFilterable
import cc.drny.lanzou.event.Scrollable
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class FileManagerFragment : Fragment(), FileFilterable {

    private var _binding: FragmentFileManagerBinding? = null
    private val binding get() = _binding!!

    private val titles = arrayOf("根目录", "数据", "QQ", "微信", "下载", "相册", "截图", "录屏", "文档")
    private val paths = arrayOf(
        "/storage/emulated/0",
        "/storage/emulated/0/Android/data",
        "/storage/emulated/0/Android/data/com.tencent.mobileqq/Tencent/QQfile_recv",
        "/storage/emulated/0/Android/data/com.tencent.mm/MicroMsg/Download",
        "/storage/emulated/0/Download",
        "/storage/emulated/0/DCIM/Camera",
        "/storage/emulated/0/DCIM/Screenshots",
        "/storage/emulated/0/DCIM/ScreenRecorder",
        "/storage/emulated/0/Document"
    )

    private val fragments = mutableListOf<Fragment>()

    private lateinit var pageAdapter: ViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        for (position in titles.indices) {
            fragments.add(if (position == 1) AppDataFragment()
            else if ((position == 2 || position == 3) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                AndroidDataFragment().apply {
                    arguments = bundleOf("path" to paths[position])
                }
            } else {
                LocalFileFragment().apply {
                    arguments = bundleOf("path" to paths[position], "name" to titles[position])
                }
            })
        }

        if (savedInstanceState != null) {
            childFragmentManager.fragments.forEach { old ->
                fragments.forEachIndexed { index, new ->
                    if (new::class.simpleName == old::class.simpleName) {
                        if (new !is AppDataFragment) {
                            val oldPath = old.requireArguments().getString("path")
                            val newPath = new.requireArguments().getString("path")
                            if (oldPath == newPath) {
                                Log.d("jdy", "index: $index")
                                fragments[index] = old
                            }
                        } else {
                            fragments[index] = old
                        }
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
        _binding = FragmentFileManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pageAdapter = ViewPagerAdapter()
        binding.vpFileManager.adapter = pageAdapter

        // binding.vpFileManager.offscreenPageLimit = titles.size
        TabLayoutMediator(binding.tabLayoutFileManager, binding.vpFileManager) { tab, position ->
            tab.text = titles[position]
        }.attach()

        binding.tabLayoutFileManager.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {
                val fragment = fragments[tab.position]
                if (fragment is Scrollable) {
                    fragment.scrollToFirst()
                }
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }
        })
    }

    override fun onFilter(key: String?) {
        if (_binding == null) return
        val fragment = fragments[binding.vpFileManager.currentItem]
        if (fragment is FileFilterable) {
            fragment.onFilter(key)
        }
    }

    private inner class ViewPagerAdapter : FragmentStateAdapter(childFragmentManager, lifecycle) {

        override fun createFragment(position: Int): Fragment = fragments[position]

        override fun getItemCount() = fragments.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}