package ru.vasiliev.sandbox

import android.app.Application
import ru.vasiliev.sandbox.common.util.Clerk

import ru.vasiliev.sandbox.di.components.AppComponent
import ru.vasiliev.sandbox.di.components.DaggerAppComponent
import ru.vasiliev.sandbox.di.modules.AppModule
import timber.log.Timber

/**
 * Date: 04.08.2019
 *
 * @author Kirill Vasiliev
 */
class App : Application() {


    companion object {

        lateinit var instance: App
            private set

        lateinit var appComponent: AppComponent
            private set

        val clerk = Clerk("AndroidSandbox")
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        init()
    }

    private fun init() {
        appComponent = DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build()
        Timber.plant(Timber.DebugTree())
    }
}
