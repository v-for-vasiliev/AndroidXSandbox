package ru.vasiliev.sandbox

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import butterknife.ButterKnife
import butterknife.OnClick
import ru.vasiliev.sandbox.browser.BrowserActivity
import ru.vasiliev.sandbox.camera.data.action.CameraAction
import ru.vasiliev.sandbox.camera.data.action.CameraActionKind
import ru.vasiliev.sandbox.camera.presentation.camera.CameraActivity
import ru.vasiliev.sandbox.camera.presentation.newcameratest.NewCameraTestActivity
import java.util.*

/**
 * Date: 04.08.2019
 *
 * @author Kirill Vasiliev
 */
class MainActivity : AppCompatActivity() {

    @OnClick(R.id.location, R.id.camera, R.id.view, R.id.browser)
    fun onClick(view: View) {
        when (view.id) {
            R.id.location -> openLocation()
            R.id.camera -> openCamera()
            R.id.view -> openViewActivity()
            R.id.browser -> openBrowser()
            else -> {
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        ButterKnife.bind(this)
    }

    private fun openLocation() {
        val intent = Intent(this@MainActivity, LocationActivity::class.java)
        // intent.putExtra(KEY_PROVIDER_TYPE, PROVIDER_TYPE_FUSED);
        startActivity(intent)
    }

    private fun openCamera() {
        NewCameraTestActivity.start(this)
    }

    private fun openLegacyCamera() {
        val actions = ArrayList<CameraAction>()

        actions.add(CameraAction.Builder(CameraActionKind.PHOTO_AND_BARCODE)
                        .setCaptureId(0)
                        .setCaptureQuality(70)
                        .setScanId(0)
                        .setScanPattern("\\d{12}")
                        .setIndex(1)
                        .setOrder(0)
                        .setDescription("ПСК")
                        .build())

        actions.add(CameraAction.Builder(CameraActionKind.PHOTO)
                        .setCaptureId(2)
                        .setCaptureQuality(90)
                        .setOrder(2)
                        .setIndex(1)
                        .setDescription("Фото клиента")
                        .build())

        actions.add(CameraAction.Builder(CameraActionKind.BARCODE)
                        .setScanId(2)
                        .setScanPattern("\\d{12}")
                        .setIndex(1)
                        .setOrder(3)
                        .setDescription("Анкета (штрих-код)")
                        .build())

        CameraActivity.start(this, actions, CameraActivity.RequestKind.REQUEST_KIND_MIXED)
    }

    private fun openViewActivity() {
        ViewActivity.start(this@MainActivity)
    }

    private fun openBrowser() {
        // requestPermissionsAndTakePicture("https://webrtc.github.io/samples/src/content/getusermedia/gum")
        // requestPermissionsAndTakePicture("file:///android_asset/webrtc/content/index.html")
        // requestPermissionsAndTakePicture("https://moneyman.ru/lp/main/?partner=Sovest-1&wmid=901")
        // requestPermissionsAndTakePicture("")
        BrowserActivity.start(this@MainActivity, "https://valor-software.com/ng2-file-upload")
        // BrowserActivity.start(this@MainActivity, "https://angular-file-upload.appspot.com")
    }
}
