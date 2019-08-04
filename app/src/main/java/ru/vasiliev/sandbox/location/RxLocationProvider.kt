package ru.vasiliev.sandbox.location

import android.location.Location

import io.reactivex.Observable

/**
 * Date: 29.06.2019
 *
 * @author Kirill Vasiliev
 */
interface RxLocationProvider {

    companion object {

        val PROVIDER_TYPE_LEGACY = 1

        val PROVIDER_TYPE_FUSED = 2

        val TRACKING_INTERVAL_SECONDS = 60 // 1 minute
    }

    fun isRequestingUpdates(): Boolean

    fun getLastKnownLocation(): Observable<Location>

    fun getLocationObservable(): Observable<Location>

    fun getLocationHistoryObservable(): Observable<Location>

    fun isTracking(): Boolean

    fun setCallback(callback: RxLocationCallback)

    fun removeCallback()

    fun start()

    fun stop()

    fun startTracking()

    fun stopTracking()
}
