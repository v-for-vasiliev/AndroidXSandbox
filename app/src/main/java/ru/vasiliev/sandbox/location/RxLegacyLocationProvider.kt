package ru.vasiliev.sandbox.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.SettingsClient
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import timber.log.Timber

import java.util.concurrent.TimeUnit

/**
 * Date: 04.08.2019
 *
 * @author Kirill Vasiliev
 *
 * Precondition: user must allow location permission request. See [BaseLocationActivity]
 */

class RxLegacyLocationProvider private constructor(private val mContext: Context,
                                                   private val mProvider: String,
                                                   private val mUpdateIntervalInMilliseconds: Long,
                                                   private val mFastestUpdateIntervalInMilliseconds: Long,
                                                   private val mPriority: Int,
                                                   private var mRxLocationCallback: RxLocationCallback?) :
        RxLocationProvider {

    private var mSettingsClient: SettingsClient? = null

    private var mLocationSettingsRequest: LocationSettingsRequest? = null

    private var mLegacyLocationManager: LocationManager? = null

    private var mLegacyLocationListener: LocationListener? = null

    private var mLocationRequest: LocationRequest? = null

    private val mResultPublisher = PublishSubject.create<Location>()

    private val mResultHistoryPublisher = ReplaySubject.create<Location>()

    private var mRequestingUpdates = false

    private val mTrackingSubscriptions = CompositeDisposable()

    private var mTracking = false

    private val isInited: Boolean
        get() = (mSettingsClient != null && mLocationSettingsRequest != null && mLegacyLocationManager != null && mLocationRequest != null && mLegacyLocationListener != null)

    init {
        Timber.d("constructor() {provider=%s, updateInterval=%d, " + "fastestUpdateInterval=%d, priority=%d, hasCallback=%b}",
                 mProvider,
                 mUpdateIntervalInMilliseconds,
                 mFastestUpdateIntervalInMilliseconds,
                 mPriority,
                 mRxLocationCallback != null)
    }

    override fun setCallback(callback: RxLocationCallback) {
        Timber.d("setCallback() = $callback")
        mRxLocationCallback = callback
    }

    override fun removeCallback() {
        Timber.d("removeCallback()")
        mRxLocationCallback = null
    }

    override fun start() {
        Timber.d("start()")
        if (!isInited) {
            init()
        }
        requestLocationUpdates()
    }

    override fun stop() {
        if (isInited) {
            Timber.d("stop()")
            stopTracking()
            mLegacyLocationManager!!.removeUpdates(mLegacyLocationListener)
            mRequestingUpdates = false
            mLegacyLocationManager = null
            mLocationRequest = null
            mSettingsClient = null
            mLocationSettingsRequest = null
            mLegacyLocationListener = null

        }
    }

    override fun isRequestingUpdates(): Boolean {
        return mRequestingUpdates
    }

    @SuppressLint("MissingPermission")
    override fun getLastKnownLocation(): Observable<Location> {
        return Observable.create { subscriber ->
            val location = mLegacyLocationManager!!.getLastKnownLocation(mProvider)
            if (location != null) {
                Timber.d("getLastKnownLocation() {" + location.latitude + ": " + location.longitude + "; Accuracy: " + location.accuracy + "}")
                subscriber.onNext(location)
                subscriber.onComplete()
            } else {
                Timber.d("getLastKnownLocation() = null")
                subscriber.onError(RuntimeException("No last location"))
            }
        }
    }

    override fun getLocationObservable(): Observable<Location> {
        return mResultPublisher.hide()
    }

    /**
     * @return all location updates since provider start (use [Observable.take])
     */
    override fun getLocationHistoryObservable(): Observable<Location> {
        return mResultHistoryPublisher.hide()
    }

    override fun startTracking() {
        if (!mTracking) {
            mTracking = true
            mTrackingSubscriptions.add(Observable.interval(0,
                                                           RxLocationProvider.TRACKING_INTERVAL_SECONDS.toLong(),
                                                           TimeUnit.SECONDS).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({ sendDsaLocation() },
                                                                                                                                                              { e ->
                                                                                                                                                                  mTracking =
                                                                                                                                                                          false
                                                                                                                                                                  Timber.e("",
                                                                                                                                                                           e)
                                                                                                                                                              },
                                                                                                                                                              { Timber.d("Tracking stopped") }))
        }
    }

    override fun stopTracking() {
        if (mTracking) {
            Timber.d("stopTracking()")
            mTrackingSubscriptions.clear()
            mTracking = false
        }
    }

    override fun isTracking(): Boolean {
        return mTracking
    }

    private fun init() {
        Timber.d("init()")
        mLegacyLocationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        mLocationRequest = createLocationRequest()
        mSettingsClient = com.google.android.gms.location.LocationServices.getSettingsClient(mContext)
        mLocationSettingsRequest = buildLocationSettingsRequest(mLocationRequest)
        mLegacyLocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location?) {
                if (location != null) {
                    pushNewLocation(location)
                } else {
                    Timber.d("onLocationChanged() = null")
                }
            }

            override fun onStatusChanged(provider: String,
                                         status: Int,
                                         extras: Bundle) {
                Timber.d("onStatusChanged() {provider=%s, status=%d, extras={%s}}",
                         provider,
                         status,
                         extras)
            }

            @SuppressLint("MissingPermission")
            override fun onProviderEnabled(provider: String) {
                Timber.d("onProviderEnabled() {provider=%s}",
                         provider)
                val location = mLegacyLocationManager!!.getLastKnownLocation(provider)
                if (location != null) {
                    pushNewLocation(location)
                }
            }

            override fun onProviderDisabled(provider: String) {
                Timber.d("onProviderDisabled() {provider=%s}",
                         provider)
            }
        }
    }

    private fun createLocationRequest(): LocationRequest {
        val locationRequest = LocationRequest()
        locationRequest.interval = mUpdateIntervalInMilliseconds
        locationRequest.fastestInterval = mFastestUpdateIntervalInMilliseconds
        locationRequest.priority = mPriority
        return locationRequest
    }

    private fun buildLocationSettingsRequest(locationRequest: LocationRequest?): LocationSettingsRequest {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest!!)
        return builder.build()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        if (!mRequestingUpdates) {
            Timber.d("requestLocationUpdates()")
            mRequestingUpdates = true
            mSettingsClient!!.checkLocationSettings(mLocationSettingsRequest)
                    .addOnSuccessListener {
                        mLegacyLocationManager!!.requestLocationUpdates(mProvider,
                                                                        mFastestUpdateIntervalInMilliseconds,
                                                                        0.0f,
                                                                        mLegacyLocationListener)
                    }
                    .addOnFailureListener { e ->
                        mRequestingUpdates = false
                        if (mRxLocationCallback != null) {
                            mRxLocationCallback!!.onLocationSettingsError(e)
                        }
                    }
        }
    }

    private fun pushNewLocation(location: Location) {
        Timber.d("onLocationChanged() {" + location.latitude + ": " + location.longitude + "; Accuracy: " + location.accuracy + "}")
        mResultPublisher.onNext(location)
        mResultHistoryPublisher.onNext(location)
        if (mRxLocationCallback != null) {
            mRxLocationCallback!!.onLocationChange(location)
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendDsaLocation() {
        val location = mLegacyLocationManager!!.getLastKnownLocation(mProvider)
        if (location != null) {
            // Send location to server (mTrackingSubscriptions)
            Timber.d("sendDsaLocation() {" + location.latitude + ": " + location.longitude + "; Accuracy: " + location.accuracy + "}")
        } else {
            Timber.d("sendDsaLocation() skipped, no location")
        }
    }

    class Builder(private var context: Context) {

        private var provider = LocationManager.NETWORK_PROVIDER

        private var updateInterval: Long = 10000

        private var fastestUpdateInterval = updateInterval / 2

        private var priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        private var rxLocationCallback: RxLocationCallback? = null

        fun context(context: Context): Builder {
            this.context = context
            return this
        }

        /**
         * @param provider [LocationManager.NETWORK_PROVIDER] or [                 ][LocationManager.GPS_PROVIDER]
         */
        fun provider(provider: String): Builder {
            this.provider = provider
            return this
        }

        fun updateIntervalMilliseconds(updateInterval: Long): Builder {
            this.updateInterval = updateInterval
            return this
        }

        fun fastestIntervalMilliseconds(fastestUpdateInterval: Long): Builder {
            this.fastestUpdateInterval = fastestUpdateInterval
            return this
        }

        fun priority(priority: Int): Builder {
            this.priority = priority
            return this
        }

        fun callback(callback: RxLocationCallback): Builder {
            this.rxLocationCallback = callback
            return this
        }

        fun build(): RxLegacyLocationProvider {
            return RxLegacyLocationProvider(context,
                                            provider,
                                            updateInterval,
                                            fastestUpdateInterval,
                                            priority,
                                            rxLocationCallback)
        }
    }
}