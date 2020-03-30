package ru.vasiliev.sandbox.browser

import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import timber.log.Timber

class MfoWebChromeClient : WebChromeClient() {

    override fun onPermissionRequest(request: PermissionRequest?) {
        request?.grant(request.resources)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        Timber.d("MfoWebChromeClient(%s:%d): %s",
                 consoleMessage?.sourceId(),
                 consoleMessage?.lineNumber(),
                 consoleMessage?.message())
        return true
    }
}