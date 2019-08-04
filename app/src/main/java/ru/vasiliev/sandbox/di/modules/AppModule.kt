package ru.vasiliev.sandbox.di.modules

import dagger.Module
import dagger.Provides
import ru.vasiliev.sandbox.App
import ru.vasiliev.sandbox.di.scopes.AppScope

/**
 * Date: 04.08.2019
 *
 * @author Kirill Vasiliev
 */
@Module
class AppModule(private val mApp: App) {

    @AppScope
    @Provides
    fun provideApp(): App {
        return mApp
    }
}