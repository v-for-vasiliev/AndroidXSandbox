package ru.vasiliev.sandbox.browser

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import ru.vasiliev.sandbox.R

class BrowserActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)

        if (savedInstanceState == null) {
            var link = intent.getStringExtra(EXTRA_LINK)
                .orEmpty()

            /*
            if (link.isEmpty() && Utils.hasDeepLink(intent)) {
                link = intent.data?.toString().orEmpty()
            }
            */

            //val fragment = BrowserFragment.newInstance(link)
            val fragment = MfoBrowserFragment.newInstance(link)
            supportFragmentManager.beginTransaction()
                .run {
                    replace(R.id.container, fragment, BrowserFragment.TAG)
                }
                .commit()
        }
    }

    companion object {
        const val REQUEST_CODE = 500

        private const val EXTRA_LINK = "extra_link"

        @JvmStatic
        fun start(context: Context, link: String) {
            val intent = Intent(context, BrowserActivity::class.java).apply {
                putExtra(EXTRA_LINK, link)
            }
            context.startActivity(intent)
        }

        @JvmStatic
        fun startForResult(activity: Activity, link: String) {
            val intent = Intent(activity, BrowserActivity::class.java).apply {
                putExtra(EXTRA_LINK, link)
            }
            activity.startActivityForResult(intent, REQUEST_CODE)
        }

        @JvmStatic
        fun startForResult(fragment: Fragment, link: String) {
            val intent = Intent(fragment.context, BrowserActivity::class.java).apply {
                putExtra(EXTRA_LINK, link)
            }
            fragment.startActivityForResult(intent, REQUEST_CODE)
        }
    }
}
