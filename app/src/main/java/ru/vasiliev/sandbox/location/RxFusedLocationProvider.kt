package ru.vasiliev.sandbox.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationServices
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import ru.vasiliev.sandbox.location.RxLocationCallback as RxLocationCallback1

/**
 * Date: 29.06.2019
 *
 * @author Kirill Vasiliev
 *
 * Precondition: user must allow location permission request. See [BaseLocationActivity]
 *
 * Fused location provider uses all available sources. Sources choose strategy based on priority
 * (Ex. [LocationRequest.PRIORITY_HIGH_ACCURACY]). Location updates comes every ~10 min., so
 * it not suited for fast location detection from cold start. For this case use
 * [RxLegacyLocationProvider]
 */
class RxFusedLocationProvider private constructor(private val mContext: Context,
                                                  private val mUpdateIntervalInMilliseconds: Long,
                                                  private val mFastestUpdateIntervalInMilliseconds: Long,
                                                  private val mPriority: Int,
                                                  private var mRxLocationCallback: RxLocationCallback1?) :
        RxLocationProvider {
    private var mSettingsClient: SettingsClient? = null

    private var mLocationSettingsRequest: LocationSettingsRequest? = null

    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null

    private var mLocationCallback: LocationCallback? = null

    private var mLocationRequest: LocationRequest? = null

    private val mResultPublisher = PublishSubject.create<Location>()

    private val mResultHistoryPublisher = ReplaySubject.create<Location>()

    private var mRequestingUpdates = false

    private val mTrackingSubscriptions = CompositeDisposable()

    private var mTracking = false

    private val isInited: Boolean
        get() = (mSettingsClient != null && mLocationSettingsRequest != null && mFusedLocationProviderClient != null && mLocationRequest != null && mLocationCallback != null)

    init {
        Timber.d("constructor() {updateInterval=%d, fastestUpdateInterval=%d, " + "priority=%d, hasCallback=%b}",
                mUpdateIntervalInMilliseconds, mFastestUpdateIntervalInMilliseconds, mPriority,
                mRxLocationCallback != null)
    }


    override fun setCallback(callback: RxLocationCallback1) {
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
            mFusedLocationProviderClient!!.removeLocationUpdates(mLocationCallback!!)
                    .addOnCompleteListener {
                        mRequestingUpdates = false
                        mFusedLocationProviderClient = null
                        mLocationRequest = null
                        mSettingsClient = null
                        mLocationSettingsRequest = null
                        mLocationCallback = null
                    }

        }
    }

    override fun isRequestingUpdates(): Boolean {
        return mRequestingUpdates
    }

    @SuppressLint("MissingPermission")
    override fun getLastKnownLocation(): Observable<Location> {
        return Observable.create { subscriber ->
            mFusedLocationProviderClient!!.lastLocation.addOnSuccessListener { location ->
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    Timber.d(
                            "getLastKnownLocation() {" + location.latitude + ": " + location.longitude + "; Accuracy: " + location.accuracy + "}")
                    subscriber.onNext(location)
                    subscriber.isDisposed
                } else {
                    Timber.d("getLastKnownLocation() = null")
                    subscriber.onError(RuntimeException("No last location"))
                }
            }
                    .addOnFailureListener {
                        Timber.d("getLastKnownLocation() = null")
                        subscriber.onError(RuntimeException("No last location"))
                    }
        }
    }

    override fun getLocationObservable(): Observable<Location> {
        return mResultPublisher.hide()
    }

    /**
     * @return all location updates since provider start (use [Observable.take]})
     */
    override fun getLocationHistoryObservable(): Observable<Location> {
        return mResultHistoryPublisher.hide()
    }

    override fun startTracking() {
        if (!mTracking) {
            Timber.d("startTracking()")
            mTracking = true
            mTrackingSubscriptions.add(Observable.interval(0, RxLocationProvider.TRACKING_INTERVAL_SECONDS.toLong(),
                    TimeUnit.SECONDS).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                    { sendDsaLocation() }, { e ->
                mTracking = false
                Timber.e("", e)
            }, { Timber.d("Tracking stopped") }))
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
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mContext)
        mLocationRequest = createLocationRequest()
        mSettingsClient = LocationServices.getSettingsClient(mContext)
        mLocationSettingsRequest = buildLocationSettingsRequest(mLocationRequest)
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                val lastLocation = locationResult!!.lastLocation
                if (lastLocation != null) {
                    pushNewLocation(lastLocation)
                } else {
                    Timber.d("onLocationResult() = null")
                }
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
                        mFusedLocationProviderClient!!.requestLocationUpdates(mLocationRequest, mLocationCallback!!,
                                Looper.myLooper())
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
        Timber.d(
                "onLocationChanged() {" + location.latitude + ": " + location.longitude + "; Accuracy: " + location.accuracy + "}")
        mResultPublisher.onNext(location)
        mResultHistoryPublisher.onNext(location)
        if (mRxLocationCallback != null) {
            mRxLocationCallback!!.onLocationChange(location)
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendDsaLocation() {
        mFusedLocationProviderClient!!.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // Send location to server (mTrackingSubscriptions)
                Timber.d(
                        "sendDsaLocation() {" + location.latitude + ": " + location.longitude + "; Accuracy: " + location.accuracy + "}")
            } else {
                Timber.d("sendDsaLocation() skipped, no location")
            }
        }
                .addOnFailureListener { Timber.d("sendDsaLocation() skipped, no location") }
    }

    class Builder(private val context: Context) {

        private var updateInterval: Long = 10000

        private var fastestUpdateInterval = updateInterval / 2

        private var priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        private var rxLocationCallback: RxLocationCallback1? = null

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

        fun callback(callback: RxLocationCallback1): Builder {
            this.rxLocationCallback = callback
            return this
        }

        fun build(): RxFusedLocationProvider {
            return RxFusedLocationProvider(context, updateInterval, fastestUpdateInterval, priority, rxLocationCallback)
        }
    }
}