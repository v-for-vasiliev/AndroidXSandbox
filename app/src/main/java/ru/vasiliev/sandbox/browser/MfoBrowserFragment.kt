package ru.vasiliev.sandbox.browser

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.webkit.*
import androidx.core.os.bundleOf
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.fragment_browser.*
import ru.vasiliev.sandbox.BuildConfig
import ru.vasiliev.sandbox.common.util.IoUtils
import ru.vasiliev.sandbox.security.FileProviderHelper
import ru.vasiliev.sandbox.security.FileProviderHelper.FILEPROVIDER_SECURE_IMAGE_DIR
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MfoBrowserFragment : BrowserFragment() {

    private var fileChooserPathCallback: ValueCallback<Array<Uri>>? = null
    private var cameraImagePath: String? = null

    override fun loadUrl() {
        val link = arguments?.getString(EXTRA_LINK)
            .orEmpty()
        Dexter.withActivity(activity)
            .withPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        webView.loadUrl(link)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<com.karumi.dexter.listener.PermissionRequest>?,
                        token: PermissionToken?) {
                    with(AlertDialog.Builder(context)) {
                        setTitle("Браузер")
                        setMessage("Для коректной работы браузера необходимы разрешения на работу с камерой и хранилищем.")
                        setPositiveButton("Настройки") { _, _ ->
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                 Uri.parse("package:" + BuildConfig.APPLICATION_ID)))
                        }
                        setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
                        show()
                    }
                }
            })
            .check()
    }

    override fun createWebChromeClient(): WebChromeClient = object : WebChromeClient() {

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

        override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>?,
                                       fileChooserParams: FileChooserParams?): Boolean {
            if (!isAdded || activity == null) {
                return true
            }

            fileChooserPathCallback?.onReceiveValue(null)
            fileChooserPathCallback = filePathCallback

            val takePhotoIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePhotoIntent?.resolveActivity(activity!!.packageManager) != null) {
                val imageFile = createCameraImageFile()
                if (imageFile != null) {
                    val secureImageUri = FileProviderHelper.getSecureImageUri(activity!!, imageFile)
                    cameraImagePath = "file://" + imageFile.absolutePath
                    takePhotoIntent.putExtra(EXTRA_IMAGE_PATH, cameraImagePath)
                    takePhotoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    takePhotoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, secureImageUri)

                    val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                    contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                    contentSelectionIntent.type = "image/*"

                    val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                    chooserIntent.putExtra(Intent.EXTRA_TITLE, "Выбор файла")
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(takePhotoIntent))

                    startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST_CODE)
                }
            }

            return true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != FILE_CHOOSER_REQUEST_CODE || fileChooserPathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }
        var results: Array<Uri>? = null
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                // If there is no data, then we may have taken a photo
                if (cameraImagePath != null) {
                    results = arrayOf(Uri.parse(cameraImagePath))
                }
            } else {
                val dataString = data.dataString
                if (dataString != null) {
                    results = arrayOf(Uri.parse(dataString))
                }
            }
            results?.let {
                fileChooserPathCallback!!.onReceiveValue(it)
                fileChooserPathCallback = null
            }
        } else {
            fileChooserPathCallback!!.onReceiveValue(null)
            fileChooserPathCallback = null
        }
    }

    private fun createCameraImageFile(): File? {
        try {
            val timeStamp: String = SimpleDateFormat("yyyy.MM.dd_HH.mm.ss", Locale.getDefault()).format(Date())
            val fileName = "mfo_passport_$timeStamp"
            val storageDir = File(activity!!.filesDir, FILEPROVIDER_SECURE_IMAGE_DIR)
            storageDir.mkdir()
            return File.createTempFile(fileName, ".jpg", storageDir)
        } catch (e: IOException) {
            Timber.e(e, "Unable to create image file")
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        IoUtils.deleteRecursive(File(activity!!.filesDir, FILEPROVIDER_SECURE_IMAGE_DIR))
    }

    companion object {

        private const val FILE_CHOOSER_REQUEST_CODE = 501
        private const val EXTRA_IMAGE_PATH = "image_path"

        fun newInstance(link: String): MfoBrowserFragment {
            return MfoBrowserFragment().apply {
                arguments = bundleOf(EXTRA_LINK to link)
            }
        }
    }
}