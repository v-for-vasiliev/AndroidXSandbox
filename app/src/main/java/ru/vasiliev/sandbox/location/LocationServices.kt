package ru.vasiliev.sandbox.location

import android.provider.Settings

import ru.vasiliev.sandbox.App

/**
 * Date: 04.08.2019
 *
 * @author Kirill Vasiliev
 */
class LocationServices(private val mApp: App,
                       val locationProvider: RxLocationProvider) {

    protected val isLocationEnabled: Boolean
        get() {
            val locationMode = Settings.Secure.getInt(mApp.contentResolver,
                                                      Settings.Secure.LOCATION_MODE,
                                                      0)
            return locationMode != Settings.Secure.LOCATION_MODE_OFF
        }
}
