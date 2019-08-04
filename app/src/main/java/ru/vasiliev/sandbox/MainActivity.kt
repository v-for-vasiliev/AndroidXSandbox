package ru.vasiliev.sandbox

import android.content.Intent
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import butterknife.ButterKnife
import butterknife.OnClick

/**
 * Date: 04.08.2019
 *
 * @author Kirill Vasiliev
 */
class MainActivity : AppCompatActivity() {

    @OnClick(R.id.location)
    fun onLocationClick() {
        val intent = Intent(this@MainActivity, LocationActivity::class.java)
        // intent.putExtra(KEY_PROVIDER_TYPE, PROVIDER_TYPE_FUSED);
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        ButterKnife.bind(this)
    }
}
