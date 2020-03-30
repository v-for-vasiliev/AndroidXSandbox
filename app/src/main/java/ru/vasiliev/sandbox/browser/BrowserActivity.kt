package ru.vasiliev.sandbox.browser

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.activity_browser.*
import ru.vasiliev.sandbox.R

class BrowserActivity : AppCompatActivity() {

    companion object {
        fun start(activity: Activity) {
            activity.startActivity(Intent(activity, BrowserActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)
        initWebView()
        // requestPermissionsAndTakePicture("https://webrtc.github.io/samples/src/content/getusermedia/gum")
        requestPermissionsAndTakePicture("file:///android_asset/webrtc/content/index.html")
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun initWebView() {
        // Used instead Kotlin's extensions, because Web client callbacks may produce NPE
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

            clearCache(true)

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Toast.makeText(this@BrowserActivity, "Page loaded: $url", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            webChromeClient = MfoWebChromeClient()

            addJavascriptInterface(BrowserJsInterface(), "Android")
        }
    }

    private fun requestPermissionsAndTakePicture(url: String) {
        Dexter.withActivity(this)
            .withPermission(Manifest.permission.CAMERA)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    webView.loadUrl(url)
                    // webView.loadData("<div style=\"text-align:center;\"><input type=\"file\" accept=\"image/*;capture=camera\"></div>", "text/html", "UTF-8")
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {}
                override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest, token: PermissionToken) {
                }
            })
            .check()
    }
}
