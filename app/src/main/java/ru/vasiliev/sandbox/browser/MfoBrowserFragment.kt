package ru.vasiliev.sandbox.browser

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.*
import androidx.core.os.bundleOf
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.fragment_browser.*
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MfoBrowserFragment : BrowserFragment() {

    private var cachedFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var cameraPhotoPath: String? = null

    override fun loadUrl() {
        val link = arguments?.getString(EXTRA_LINK)
            .orEmpty()
        /*
        Dexter.withActivity(activity)
            .withPermission(Manifest.permission.CAMERA)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    val link = arguments?.getString(EXTRA_LINK)
                        .orEmpty()
                    // webView.loadUrl(link)
                    webView.loadData("<input type=\"file\" accept=\"image/*\" capture=\"environment\">",
                                     "text/html; charset=UTF-8",
                                     null)
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {}
                override fun onPermissionRationaleShouldBeShown(
                        permission: com.karumi.dexter.listener.PermissionRequest, token: PermissionToken) {
                }
            })
            .check()
         */
         */

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

            cachedFilePathCallback?.onReceiveValue(null)
            cachedFilePathCallback = filePathCallback

            /*
            var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent?.resolveActivity(activity!!.packageManager) != null) {
                // Create the File where the photo should go
                var photoFile: File? = null
                try {
                    photoFile = createImageFile()
                    takePictureIntent.putExtra("PhotoPath", cameraPhotoPath)
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Timber.e("Unable to create Image File", ex)
                }

                // Continue only if the File was successfully created
                if (photoFile != null) {
                    cameraPhotoPath = "content:" + photoFile.absolutePath
                    val photoUri = FileProvider.getUriForFile(context!!,
                                                              context!!.applicationContext.packageName.toString() + ".fileprovider",
                                                              photoFile)
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    takePictureIntent.data = photoUri
                } else {
                    takePictureIntent = null
                }
            }
            */

            var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent?.resolveActivity(activity!!.packageManager) != null) {
                // Create the File where the photo should go
                var photoFile: File? = null
                try {
                    photoFile = createImageFile()
                    takePictureIntent.putExtra("PhotoPath", cameraPhotoPath)
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Timber.e("Unable to create Image File", ex)
                }

                // Continue only if the File was successfully created
                if (photoFile != null) {
                    cameraPhotoPath = "file:" + photoFile.absolutePath
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
                } else {
                    takePictureIntent = null
                }
            }

            val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
            contentSelectionIntent.type = "image/*"

            val intentArray: Array<Intent?> = arrayOf(takePictureIntent)

            val chooserIntent = Intent(Intent.ACTION_CHOOSER)
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)

            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)

            return true
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyy.MM.dd_HH.mm.ss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName,  /* prefix */
                                   ".jpg",  /* suffix */
                                   storageDir /* directory */)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || cachedFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }
        var results: Array<Uri>? = null

        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                // If there is not data, then we may have taken a photo
                if (cameraPhotoPath != null) {
                    results = arrayOf(Uri.parse(cameraPhotoPath))
                }
            } else {
                val dataString = data.dataString
                if (dataString != null) {
                    results = arrayOf(Uri.parse(dataString))
                }
            }
        }
        results?.let {
            cachedFilePathCallback!!.onReceiveValue(it)
            cachedFilePathCallback = null
        }
    }

    companion object {

        private const val INPUT_FILE_REQUEST_CODE = 1
        private const val EXTRA_FROM_NOTIFICATION = "EXTRA_FROM_NOTIFICATION"

        fun newInstance(link: String): MfoBrowserFragment {
            return MfoBrowserFragment().apply {
                arguments = bundleOf(EXTRA_LINK to link)
            }
        }
    }
}