package ru.vasiliev.sandbox

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

import androidx.appcompat.widget.Toolbar
import butterknife.BindView
import butterknife.ButterKnife
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import ru.vasiliev.sandbox.location.BaseLocationActivity
import timber.log.Timber

/**
 * Date: 04.08.2019
 *
 * @author Kirill Vasiliev
 */
class LocationActivity : BaseLocationActivity() {

    @JvmField
    @BindView(R.id.output_last_location)
    var mOutputLastLocation: TextView? = null

    @JvmField
    @BindView(R.id.output_location)
    var mOutputLocation: TextView? = null

    @JvmField
    @BindView(R.id.output_location_history)
    var mOutputLocationHistory: TextView? = null

    @JvmField
    @BindView(R.id.error)
    var mOutputError: TextView? = null

    @JvmField
    @BindView(R.id.get_last_location)
    var mLastLocationButton: Button? = null

    @JvmField
    @BindView(R.id.get_location)
    var mLocationButton: Button? = null

    @JvmField
    @BindView(R.id.get_locations_history)
    var mLocationsHistoryButton: Button? = null

    val mSubscriptions = CompositeDisposable()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        ButterKnife.bind(this)

        mLastLocationButton!!.setOnClickListener {
            if (isLocationRequestingRunning) {
                mOutputLastLocation!!.text = "Requesting location..."
                mSubscriptions.add(
                        lastLocation.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                                { location ->
                                    mOutputLastLocation!!.text = (location.latitude.toString() + " : " + location.longitude + "; " + "Accuracy: " + location.accuracy)
                                }, { throwable ->
                            mOutputLastLocation!!.text = "getLastLocation(): error: " + throwable.message
                        }))
            } else {
                mOutputLastLocation!!.text = "Location monitor is not running"
            }
        }

        mLocationButton!!.setOnClickListener {
            if (isLocationRequestingRunning) {
                mOutputLocation!!.text = "Requesting location..."
                mSubscriptions.add(
                        location.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                                { location ->
                                    val locationString = location.latitude.toString() + " : " + location.longitude + "; " + "Accuracy: " + location.accuracy
                                    Timber.d("getLocation(): $locationString")
                                    mOutputLocation!!.text = locationString
                                }, { throwable ->
                            mOutputLocation!!.text = "getLocation(): error: " + throwable.message
                        }))
            } else {
                mOutputLocation!!.text = "Location monitor is not running"
            }
        }

        mLocationsHistoryButton!!.setOnClickListener {
            if (isLocationRequestingRunning) {
                mOutputLocationHistory!!.text = "Requesting location..."
                mSubscriptions.add(
                        locationHistory.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).take(
                                1).subscribe({ location ->
                            val locationString = location.latitude.toString() + " : " + location.longitude + "; " + "Accuracy: " + location.accuracy
                            Timber.d("getLocationHistory(): $locationString")
                            mOutputLocationHistory!!.text = locationString
                        }, { throwable ->
                            mOutputLocationHistory!!.text = "getLocationHistory(): error: " + throwable.message
                        }))
            } else {
                mOutputLocationHistory!!.text = "Location monitor is not running"
            }
        }
    }

    override fun onLocationChange(location: Location) {
        /*
        mOutput.setText(
                "onLocationChange(): " + location.getLatitude() + " : " + location.getLongitude()
                        + "\n" + "Accuracy: " + location.getAccuracy());
                        */
    }

    override fun onLocationSettingsUnresolvableError(e: Exception) {
        mOutputError!!.text = e.message
    }

    override fun onPlayServicesUnresolvableError() {
        mOutputError!!.text = "onPlayServicesUnresolvableError"
    }

    override fun onLocationPermissionDenied() {
        mOutputError!!.text = "onLocationPermissionDenied"
    }

    override fun onDestroy() {
        super.onDestroy()
        mSubscriptions.clear()
    }
}
