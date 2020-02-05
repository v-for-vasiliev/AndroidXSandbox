package ru.vasiliev.sandbox.di.components

import dagger.Component
import ru.vasiliev.sandbox.App
import ru.vasiliev.sandbox.di.modules.AppModule
import ru.vasiliev.sandbox.di.modules.LocationModule
import ru.vasiliev.sandbox.di.scopes.AppScope
import ru.vasiliev.sandbox.location.LocationServices

/**
 * Date: 04.08.2019
 *
 * @author Kirill Vasiliev
 */
@AppScope
@Component(modules = [AppModule::class, LocationModule::class])
interface AppComponent {

    val app: App

    val locationServices: LocationServices

    val cameraComponent: CameraComponent
}
