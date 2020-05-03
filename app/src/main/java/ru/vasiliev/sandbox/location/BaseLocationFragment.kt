package ru.vasiliev.sandbox.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes

abstract class BaseLocationFragment : Fragment(), ConnectionCallbacks, OnConnectionFailedListener {
    private var googleApiClient: GoogleApiClient? = null
    private var locationSettingsRequest: LocationSettingsRequest? = null
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    override fun onResume() {
        super.onResume()
        if (isLocationEnabled) {
            connectToGoogleLocationServices()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isLocationEnabled && googleApiClient != null && googleApiClient!!.isConnected) {
            googleApiClient!!.disconnect()
            stopLocationUpdates()
        }
    }

    abstract val isLocationEnabled: Boolean

    abstract fun onLocationUpdate(location: Location?)

    private fun init() {
        activity?.let { act ->
            googleApiClient = GoogleApiClient.Builder(act)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build()

            locationManager = act.getSystemService(Context.LOCATION_SERVICE) as LocationManager


            locationSettingsRequest = buildLocationSettingsRequest(createLocationRequest())
            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    onLocationUpdate(location)
                }

                override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

                @SuppressLint("MissingPermission")
                override fun onProviderEnabled(provider: String) {
                    locationManager?.getLastKnownLocation(provider)
                        ?.let { onLocationUpdate(it) }
                }

                override fun onProviderDisabled(provider: String) {}
            }
        }
    }

    private fun connectToGoogleLocationServices() {
        if (!checkPlayServices()) {
            // Need to install Google Play Services to use the App properly
            onPlayServicesUnresolvableError()
        } else {
            googleApiClient!!.connect()
        }
    }

    private fun checkPlayServices(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(activity)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
            } else {
                return false
            }
            return false
        }
        return true
    }

    override fun onConnected(bundle: Bundle?) {
        activity?.let { act ->
            if (ActivityCompat.checkSelfPermission(act,
                                                   Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            act,
                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(act,
                                                                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    showPermissionsRationale()
                } else {
                    // No explanation needed; request the permission
                    ActivityCompat.requestPermissions(act,
                                                      arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                                                              Manifest.permission.ACCESS_FINE_LOCATION),
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

    }

    override fun onConnectionSuspended(i: Int) {
        // Failed to connect to Play Services
        onPlayServicesUnresolvableError()
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        onPlayServicesUnresolvableError()
        // Failed to connect to Play Services
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_LOCATION) { // If request is cancelled, the result arrays are empty.
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted
                startLocationUpdates()
            } else {
                // Permission denied, boo! Disable the
                // functionality that depends on this permission.
                showPermissionsRationale()
            }
            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        activity?.let { act ->
            val settingsClient = LocationServices.getSettingsClient(act)
            settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener {
                    locationManager?.requestLocationUpdates(PROVIDER,
                                                            FASTEST_UPDATE_INTERVAL_MS,
                                                            0.0f,
                                                            locationListener)
                }
                .addOnFailureListener { e: Exception -> onLocationSettingsError(e) }
        }
    }

    private fun stopLocationUpdates() {
        locationManager?.removeUpdates(locationListener)
    }

    private fun createLocationRequest(): LocationRequest {
        val locationRequest = LocationRequest()
        locationRequest.interval = UPDATE_INTERVAL_MS
        locationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_MS
        locationRequest.priority = UPDATES_PRIORITY
        return locationRequest
    }

    private fun buildLocationSettingsRequest(locationRequest: LocationRequest): LocationSettingsRequest {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        return builder.build()
    }

    private fun onLocationSettingsError(e: Exception) {
        when ((e as ApiException).statusCode) {
            LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                val rae = e as ResolvableApiException
                rae.startResolutionForResult(this@BaseLocationFragment.activity, REQUEST_CODE_LOCATION_SETTINGS)
            } catch (sie: SendIntentException) {
                onLocationSettingsUnresolvableError(sie)
            }
            LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> onLocationSettingsUnresolvableError(
                    RuntimeException("SETTINGS_CHANGE_UNAVAILABLE"))
        }
    }

    private fun onLocationSettingsUnresolvableError(e: Exception?) {
        activity?.let { act ->
            AlertDialog.Builder(act)
                .setTitle("Геолокация")
                .setMessage("Необходимо включить сервисы геолокации в настройках системы")
                .setPositiveButton("Настройки") { dialog: DialogInterface, which: Int ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    dialog.dismiss()
                }
                .setNegativeButton("Отмена") { dialog: DialogInterface, which: Int -> dialog.dismiss() }
                .setCancelable(false)
                .create()
                .show()
        }

    }

    private fun onPlayServicesUnresolvableError() {
        activity?.let { act ->
            AlertDialog.Builder(act)
                .setTitle("Геолокация")
                .setMessage("На устройстве не установлены компоненты, для определения геолокации (Google Play Services)")
                .setPositiveButton("Настройки") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .setNegativeButton("Выход") { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    act.finishAffinity()
                }
                .setCancelable(false)
                .create()
                .show()
        }
    }

    private fun showPermissionsRationale() {
        activity?.let { act ->
            AlertDialog.Builder(act)
                .setTitle("Геолокация")
                .setMessage("Необходимо разрешить доступ к сервисам геолокации в настройках системы")
                .setPositiveButton("Настройки") { dialog: DialogInterface, _: Int ->
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                         Uri.fromParts("package", context!!.packageName, null)))
                    dialog.dismiss()
                }
                .setNegativeButton("Отмена") { dialog: DialogInterface, which: Int -> dialog.dismiss() }
                .setCancelable(false)
                .create()
                .show()
        }
    }

    companion object {
        const val REQUEST_CODE_LOCATION_SETTINGS = 1000
        private const val REQUEST_CODE_LOCATION = 100
        private const val PLAY_SERVICES_RESOLUTION_REQUEST = 200
        private const val PROVIDER = LocationManager.NETWORK_PROVIDER
        private const val UPDATE_INTERVAL_MS: Long = 10000
        private const val FASTEST_UPDATE_INTERVAL_MS = UPDATE_INTERVAL_MS / 2
        private const val UPDATES_PRIORITY = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
    }
}