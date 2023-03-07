package cc.drny.lanzou

import android.Manifest
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.*
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.widget.PopupWindow
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.*
import androidx.preference.PreferenceManager
import cc.drny.lanzou.base.BaseFragment
import cc.drny.lanzou.data.lanzou.LanzouShareFile
import cc.drny.lanzou.databinding.ActivityMainBinding
import cc.drny.lanzou.databinding.PopupClipdataBinding
import cc.drny.lanzou.network.LanzouRepository
import cc.drny.lanzou.service.UploadService
import cc.drny.lanzou.ui.file.FileFragmentDirections
import cc.drny.lanzou.ui.upload.UploadFileFragmentDirections
import cc.drny.lanzou.util.dp2px
import cc.drny.lanzou.util.getFiles
import cc.drny.lanzou.util.showToast
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.permissionx.guolindev.PermissionX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), ServiceConnection {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    /**
     * 自定义标题视图布局参数
     */
    private val actionBarLayoutParams = ActionBar.LayoutParams(
        ActionBar.LayoutParams.MATCH_PARENT,
        ActionBar.LayoutParams.WRAP_CONTENT
    )

    private lateinit var uploadService: UploadService

    /**
     * 默认展开标题栏时的高度
     */
    private val actionBarHeight = 120.dp2px()

    /**
     * 储存剪切板中的数据
     */
    private val primaryClipDatas = mutableListOf<String>()

    private val clipboardManager by lazy {
        getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    }

    private lateinit var sharedPreferences: SharedPreferences

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        uploadService = (service as UploadService.UploadBinder).getService()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        bindService(Intent(this, UploadService::class.java), this, BIND_AUTO_CREATE)

        initView(savedInstanceState)

        // 请求一些必要的权限
        requestPermissions()

        checkUpdate()

        getIntentData(intent)

        val data = mutableListOf("111", "222")
        Test(data).test()

        Log.d("jdy", "data: $data")
    }

    fun appBar() = binding.appBar

    fun toolBarLayout() = binding.toolbarLayout

    private fun initView(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHostFragment.navController
        appBarConfiguration =
            AppBarConfiguration(
                setOf(
                    R.id.fileFragment,
                    R.id.transmissionFragment,
                    R.id.userFragment,
                    R.id.settingFragment
                )
            )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)

        initEvent(navController, navHostFragment, savedInstanceState)
    }

    private fun initEvent(
        navController: NavController,
        navHostFragment: NavHostFragment,
        savedInstanceState: Bundle?
    ) {
        if (savedInstanceState != null) {
            // 旋转屏幕或者软件软重启时
            getActionBarView(navHostFragment)
        }

        navHostFragment.childFragmentManager.addOnBackStackChangedListener {
            getActionBarView(navHostFragment)
        }

        binding.bottomNav.setOnItemSelectedListener {
            it.isChecked = true
            NavigationUI.onNavDestinationSelected(it, navController)
        }

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            val isTopLevDes = appBarConfiguration.topLevelDestinations.contains(destination.id)
            binding.fab.isInvisible = !isTopLevDes
            binding.toolbarLayout.title =
                arguments?.getCharSequence("name", destination.label) ?: destination.label

            if (!isShowBottomNav()) {
                showBottomNav()
            }
        }

        binding.fab.setOnClickListener {
            navController.navigate(UploadFileFragmentDirections.actionGlobalUploadFileFragment())
        }
    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return
        getClipData()
    }

    /**
     * 读取剪切板
     */
    private fun getClipData() {
        if (!sharedPreferences.getBoolean("check_clip", true)) {
            return
        }
        if (clipboardManager.hasPrimaryClip()) {
            val primaryClip = clipboardManager.primaryClip ?: return
            if (primaryClip.itemCount == 0) return
            val item = primaryClip.getItemAt(0)
            val text = item.coerceToText(this).toString()
            if (primaryClipDatas.size > 10) {
                // 储存在内存中数据如果大于10条，则将数据进行清空
                primaryClipDatas.clear()
            }
            if (text in primaryClipDatas) {
                // 数据已经存取过的，不重复进行操作
                return
            }
            handleClipData(text)
        }
    }

    /**
     * 处理剪切板中的数据
     */
    private fun handleClipData(content: String, isOpen: Boolean = false) {
        val regex = Regex("(https://.+)[\\s\\n]*(密码[：:][\\s\\n]*\\S*)?[\\s\\n]*")
        val pwdRegex = Regex("密码[:：][\\s\\n]*")
        val resultSequence = regex.findAll(content)
        val lanzouShareFiles = resultSequence.map {
            val url = it.destructured.component1()
            val pwd = it.destructured.component2().replace(pwdRegex, "")
            LanzouShareFile(url, pwd.ifBlank { null })
        }.toList()
        if (lanzouShareFiles.isEmpty()) return
        if (isOpen) {
            findNavController(R.id.nav_host)
                .navigate(
                    FileFragmentDirections
                        .actionGlobalAnalyzeFileDialogFragment(lanzouShareFiles.toTypedArray())
                )
            return
        }
        primaryClipDatas.add(content)
        showClipDataPopupWindow(lanzouShareFiles)
    }

    private fun showClipDataPopupWindow(lanzouShareFiles: List<LanzouShareFile>) {
        val clipDataContentView = PopupClipdataBinding.inflate(layoutInflater)
        val popupWindow = PopupWindow(clipDataContentView.root, -2, -2)
        popupWindow.elevation = 8f
        popupWindow.setBackgroundDrawable(null)
        clipDataContentView.tvContent.text = "发现${lanzouShareFiles.size}个分享链 >"
        clipDataContentView.btnClose.setOnClickListener {
            popupWindow.dismiss()
        }
        popupWindow.showAtLocation(
            binding.bottomNav, Gravity.TOP or Gravity.START,
            50, binding.fab.y.toInt()
        )
        clipDataContentView.root.setOnClickListener {
            popupWindow.dismiss()
            findNavController(R.id.nav_host).navigate(
                FileFragmentDirections
                    .actionGlobalAnalyzeFileDialogFragment(lanzouShareFiles.toTypedArray())
            )
        }
    }

    /**
     * 是否显示了底栏
     */
    private fun isShowBottomNav() = binding.bottomNav.translationY == 0f

    /**
     * 动画显示底栏
     */
    private fun showBottomNav() {
        binding.bottomNav.animate()
            .translationY(0f)
            .setDuration(250)
            .start()
    }

    private var isCustom = false

    private var appBarHeight = 120.dp2px()

    private fun getActionBarView(navHostFragment: NavHostFragment) {
        val fragment =
            navHostFragment.childFragmentManager.primaryNavigationFragment
        binding.apply {
            val toolBarLayoutParams = (toolbarLayout.layoutParams as AppBarLayout.LayoutParams)
            val toolBarParams = (toolbar.layoutParams) as CollapsingToolbarLayout.LayoutParams
            if (customAppbar.childCount > 0) {
                customAppbar.removeAllViews()
            }

            if (fragment is BaseFragment) {
                val toolbarCustomView = fragment.getToolBarCustomView()
                if (toolbarCustomView != null) {
                    isCustom = true
                    customAppbar.isVisible = true
                    //toolbarLayout.isTitleEnabled = false
                    customAppbar.addView(toolbarCustomView)
                     //toolbarLayout.layoutParams.height = -2
                    val typeValue = TypedValue()
                    theme.resolveAttribute(
                        com.google.android.material.R.attr.colorSurface,
                        typeValue,
                        true
                    )
                    appBar.statusBarForeground = ColorDrawable(typeValue.data)
                    toolbar.setBackgroundColor(typeValue.data)
                    toolBarLayoutParams.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                            AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
                    toolBarParams.collapseMode = CollapsingToolbarLayout.LayoutParams.COLLAPSE_MODE_OFF
                    return
                }
            }
            //toolbarLayout.isTitleEnabled = true
            customAppbar.isVisible = false
            if (!isCustom) return
            isCustom = false
             // toolbarLayout.layoutParams.height = appBarHeight
            binding.toolbar.background = null
            appBar.statusBarForeground = null
            customAppbar.isVisible = false
            toolBarLayoutParams.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                    AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
            toolBarParams.collapseMode = CollapsingToolbarLayout.LayoutParams.COLLAPSE_MODE_PIN
        }
    }

    /**
     * 请求软件必备的权限
     */
    private fun requestPermissions() {
        val requestList = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestList.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestList.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            requestList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        requestList.add(Manifest.permission.REQUEST_INSTALL_PACKAGES)
        PermissionX.init(this)
            .permissions(requestList)
            .onExplainRequestReason { scope, list ->
                scope.showRequestReasonDialog(list, "需要你同意授权才能正常使用", "允许", "拒绝")
            }
            .onForwardToSettings { scope, list ->
                scope.showForwardToSettingsDialog(list, "需要手动授权", "允许", "拒绝")
            }
            .explainReasonBeforeRequest()
            .request { allGranted, _, _ ->
                if (!allGranted) {
                    MaterialAlertDialogBuilder(this).apply {
                        setTitle("提示")
                        setMessage("部分权限未被授权，可能使用会出现问题，建议立即进行授权\n\n将出现的问题：无法上传文件")
                        setPositiveButton("授权") { _, _ ->
                            requestPermissions()
                        }
                        setNegativeButton("取消", null)
                        show()
                    }
                }
            }
    }

    private fun checkUpdate() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                // 这里进行检查更新
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
                LanzouRepository.checkUpdate(versionCode)
            }.onSuccess {
                if (it == null) return@onSuccess
                MaterialAlertDialogBuilder(this@MainActivity).apply {
                    setTitle("发现新版本${it.name}")
                    setMessage(it.content)
                    setPositiveButton("更新") { _, _ ->
                        startActivity(Intent(Intent.ACTION_VIEW, it.url.toUri()))
                    }
                    setNegativeButton("取消", null)
                    show()
                }
            }.onFailure {
                Log.e("jdy", it.toString())
                "检查更新失败，如获取更新请加入交流群".showToast()
            }
        }
    }

    /**
     * 得到外部传递过来的数据
     */
    private fun getIntentData(intent: Intent) {
        if (intent.data == null && intent.clipData == null) {
            return
        }
        // 这里对要上传的文件进行处理
        lifecycleScope.launchWhenResumed {
            delay(500)
            val navController = findNavController(R.id.nav_host)
            val content  = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (content != null) {
                handleClipData(content, true)
                return@launchWhenResumed
            }
            val files = withContext(Dispatchers.IO) {
                intent.getFiles()
            }?.toTypedArray()
            if (files.isNullOrEmpty()) return@launchWhenResumed
            if (navController.currentBackStackEntry?.destination?.id == R.id.uploadFileDialogFragment) {
                navController.popBackStack()
            }
            navController.navigate(
                FileFragmentDirections.actionGlobalUploadFileDialogFragment(
                    true,
                    files
                )
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("jdy", "newIntent")
        getIntentData(intent)
    }

    /**
     * 点击返回按钮触发这个方法
     */
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(this)
    }
}