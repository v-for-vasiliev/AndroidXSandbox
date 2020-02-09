package ru.vasiliev.sandbox

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.vasiliev.sandbox.view.ThermometerView

class ViewActivity : AppCompatActivity() {

    companion object {
        fun start(activity: Activity) {
            activity.startActivity(Intent(activity, ViewActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view)

        val thermometer = findViewById<ThermometerView>(R.id.thermometer)
        thermometer.setTemperature(90, Color.parseColor("#d84315"))
    }
}
