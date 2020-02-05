package ru.vasiliev.sandbox.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsStatusCodes
import io.reactivex.Observable
import ru.vasiliev.sandbox.App

/**
 * Date: 04.08.2019
 *
 * @author Kirill Vasiliev
 */
abstract class BaseLocationActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, RxLocationCallback {

    companion object {

        val REQUEST_CODE_LOCATION_SETTINGS = 1000

        val KEY_PROVIDER_TYPE = "key_provider_type"

        private val REQUEST_CODE_LOCATION = 100

        private val PLAY_SERVICES_RESOLUTION_REQUEST = 200
    }

    private var mGoogleApiClient: GoogleApiClient? = null

    var rxLocationProvider: RxLocationProvider? = null
        private set

    val isLocationRequestingRunning: Boolean
        get() = rxLocationProvider!!.isRequestingUpdates()

    val location: Observable<Location>
        get() = rxLocationProvider!!.getLocationObservable()

    val locationHistory: Observable<Location>
        get() = rxLocationProvider!!.getLocationHistoryObservable()

    val lastLocation: Observable<Location>
        get() = rxLocationProvider!!.getLastKnownLocation()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build()

        /*
        int providerType = getIntent().getIntExtra(KEY_PROVIDER_TYPE, PROVIDER_TYPE_LEGACY);
        if (providerType == PROVIDER_TYPE_FUSED) {
            mRxLocationProvider = new RxFusedLocationProvider.Builder()
                    .context(BaseLocationActivity.this).callback(this)
                    .updateIntervalMilliseconds(5000).fastestIntervalMilliseconds(2500)
                    .priority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY).build();
        } else {
            mRxLocationProvider = new RxLegacyLocationProvider.Builder()
                    .context(BaseLocationActivity.this).provider(LocationManager.NETWORK_PROVIDER)
                    .callback(this).updateIntervalMilliseconds(5000)
                    .fastestIntervalMilliseconds(2500)
                    .priority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY).build();
        }
        */

        rxLocationProvider = App.appComponent!!.locationServices.locationProvider
    }

    override fun onResume() {
        super.onResume()
        rxLocationProvider!!.setCallback(this)
        if (!checkPlayServices()) {
            // Need to install Google Play Services to use the App properly
            onPlayServicesUnresolvableError()
        } else {
            mGoogleApiClient!!.connect()
        }
    }

    override fun onPause() {
        super.onPause()
        rxLocationProvider!!.removeCallback()
        if (mGoogleApiClient != null && mGoogleApiClient!!.isConnected) {
            mGoogleApiClient!!.disconnect()
        }
        // Do not stop updates if provider acts like a background service
        //stopLocationUpdates();
    }

    abstract override fun onLocationChange(location: Location)

    abstract fun onLocationSettingsUnresolvableError(e: Exception)

    abstract fun onPlayServicesUnresolvableError()

    abstract fun onLocationPermissionDenied()

    override fun onLocationSettingsError(e: Exception) {
        val statusCode = (e as ApiException).statusCode
        when (statusCode) {
            LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                val rae = e as ResolvableApiException
                rae.startResolutionForResult(this@BaseLocationActivity, REQUEST_CODE_LOCATION_SETTINGS)
            } catch (sie: IntentSender.SendIntentException) {
                onLocationSettingsUnresolvableError(sie)
            }

            LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> onLocationSettingsUnresolvableError(
                    RuntimeException("SETTINGS_CHANGE_UNAVAILABLE"))
        }
    }

    private fun checkPlayServices(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)

        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
            } else {
                return false
            }
            return false
        }
        return true
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted
                    startLocationUpdates()
                } else {
                    // Permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    onLocationPermissionDenied()
                }
            }
        } // other 'case' lines to check for other
        // permissions this app might request.
    }

    private fun startLocationUpdates() {
        if (!rxLocationProvider!!.isRequestingUpdates()) {
            rxLocationProvider!!.start()
        }
    }

    private fun stopLocationUpdates() {
        rxLocationProvider!!.stop()
    }

    override fun onConnected(bundle: Bundle?) {
        if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                onLocationPermissionDenied()
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_CODE_LOCATION)

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
            startLocationUpdates()
        }
    }

    override fun onConnectionSuspended(i: Int) {
        // Failed to connect to Play Services
        onPlayServicesUnresolvableError()
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        onPlayServicesUnresolvableError()
        // Failed to connect to Play Services
    }
}
