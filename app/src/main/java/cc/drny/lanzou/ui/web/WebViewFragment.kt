package cc.drny.lanzou.ui.web

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.addCallback
import androidx.core.net.toUri
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import cc.drny.lanzou.LanzouApplication
import cc.drny.lanzou.MainActivity
import cc.drny.lanzou.R
import cc.drny.lanzou.databinding.FragmentWebviewBinding
import cc.drny.lanzou.network.LanzouRepository

class WebViewFragment: Fragment() {

    private var _viewBinding : FragmentWebviewBinding? = null
    private val viewBinding get() = _viewBinding!!

    private val args by navArgs<WebViewFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = FragmentWebviewBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initWebView()
        viewBinding.webView.loadUrl(args.url)

        val onBackPressedDispatcher = requireActivity().onBackPressedDispatcher
        onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (viewBinding.webView.canGoBack()) {
                viewBinding.webView.goBack()
            } else {
                findNavController().navigateUp()
            }
        }
        findNavController().setOnBackPressedDispatcher(onBackPressedDispatcher)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        viewBinding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                allowFileAccess = true
                allowContentAccess = true
                userAgentString = "PC Edg/103.0.5060.134"
                setSupportZoom(true)
                //setSupportMultipleWindows(true)
                displayZoomControls = true
                builtInZoomControls = true
                useWideViewPort = true
                layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                loadWithOverviewMode = true
                domStorageEnabled = true
            }
            setDownloadListener { url, _, _, _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            }
            webViewClient = object : WebViewClient() {

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url.toString()
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        return false
                    }
                    return true
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    viewBinding.progressHorizontal.isInvisible = false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    viewBinding.progressHorizontal.isInvisible = true
                    if (args.isLogin && url == LanzouApplication.LANZOU_HOST_FILE) {
                        val cookie = CookieManager.getInstance()
                            .getCookie(LanzouApplication.LANZOU_HOST_FILE)
                        if (cookie.contains("phpdisk_info=")) {
                            LanzouRepository.saveUserCookie(cookie)
                            val navController = findNavController()
                            val backStackEntry =
                                navController.getBackStackEntry(R.id.userFragment)
                            backStackEntry.savedStateHandle["isLogin"] = true
                            val backStackEntry2 = navController.getBackStackEntry(R.id.fileFragment)
                            backStackEntry2.savedStateHandle["isLogin"] = true
                            navController.popBackStack(R.id.loginFragment, true)
                        }
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    (requireActivity() as MainActivity).supportActionBar?.title = title
                }
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    viewBinding.progressHorizontal.progress = newProgress
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding.webView.removeAllViews()
        viewBinding.webView.destroy()
        _viewBinding = null
    }
}