package ru.vasiliev.sandbox.browser

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_browser.*
import ru.vasiliev.sandbox.R
import timber.log.Timber

open class BrowserFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.fragment_browser,
                                                                                     container,
                                                                                     false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    open fun loadUrl() {
        val link = arguments?.getString(EXTRA_LINK)
            .orEmpty()
        webView.loadUrl(link)
    }

    private fun initView() {
        buttonBack.isEnabled = false
        buttonForward.isEnabled = false

        initToolbar()
        initWebView()

        buttonBack.setOnClickListener {
            if (webView.canGoBack()) {
                webView.goBack()
            }
        }

        buttonForward.setOnClickListener {
            if (webView.canGoForward()) {
                webView.goForward()
            }
        }

        loadUrl()
    }

    private fun initToolbar() {
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        toolbar.setNavigationIcon(R.drawable.ic_close)
        toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun initWebView() {
        with(webView) {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.allowContentAccess = true
            settings.allowFileAccessFromFileURLs = true
            settings.allowUniversalAccessFromFileURLs = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            //settings.loadWithOverviewMode = true
            //settings.useWideViewPort = true
            //settings.domStorageEnabled = true
            //settings.loadsImagesAutomatically = true
            //settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            //isVerticalScrollBarEnabled = true

            clearHistory()
            clearFormData()
            clearCache(true)
            CookieManager.getInstance()
                .removeAllCookies {}

            webViewClient = createWebViewClient()
            webChromeClient = createWebChromeClient()

            addJavascriptInterface(BrowserJsInterface(), "Android")

            Timber.d("WebView {%s}", webView.settings.userAgentString)
        }
    }

    open fun createWebViewClient(): WebViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            buttonBack.isEnabled = view.canGoBack()
            buttonForward.isEnabled = view.canGoForward()
        }
    }

    open fun createWebChromeClient(): WebChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            progressBar.progress = newProgress
            progressBar.isVisible = newProgress < 100
        }
    }

    companion object {
        const val TAG = "BrowserFragment"

        const val EXTRA_LINK = "extra_link"

        fun newInstance(link: String): BrowserFragment {
            return BrowserFragment().apply {
                arguments = bundleOf(EXTRA_LINK to link)
            }
        }
    }
}