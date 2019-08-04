package ru.vasiliev.sandbox.location

import android.location.Location

/**
 * Date: 04.08.2019
 *
 * @author Kirill Vasiliev
 */
interface RxLocationCallback {

    fun onLocationChange(location: Location)

    fun onLocationSettingsError(e: Exception)
}