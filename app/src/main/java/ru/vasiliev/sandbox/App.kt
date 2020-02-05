package ru.vasiliev.sandbox

import android.app.Application

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

        var instance: App? = null
            private set

        var appComponent: AppComponent? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        init()
    }

    private fun init() {
        // Dagger application component
        appComponent = DaggerAppComponent.builder()
                .appModule(AppModule(this))
                .build()

        Timber.plant(Timber.DebugTree())
    }
}
