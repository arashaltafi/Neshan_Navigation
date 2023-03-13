package com.arash.neshan.test2.utils

import android.app.Activity
import android.content.Context
import android.content.IntentSender.SendIntentException
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.text.TextUtils
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*

object GpsUtils {

    private lateinit var googleApiClient: GoogleApiClient
    private const val REQUEST_LOCATION = 199
    private var locationManager: LocationManager? = null

    private fun getLocationManager(context: Context): LocationManager? {
        if (locationManager == null) locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager
    }

    fun enableGps(activity: Activity?) {
        if (isDisabledGps(activity)) {
//            showEnableGpsDialogPowerSave(activity);
            showEnableGpsDialogHigh(activity)
        }
    }

    fun isDisabledGps(context: Context?): Boolean {
        return if (context == null) true else !isHighAccuracy(context)
    }

    private fun isHighAccuracy(context: Context?): Boolean {
        if (context == null) return false
        return if ((!getLocationManager(context)!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || getLocationMode(context) != Settings.Secure.LOCATION_MODE_HIGH_ACCURACY) &&
            isHardwareGPSDevice(context)
        ) {
            false
        } else getLocationMode(context) != Settings.Secure.LOCATION_MODE_OFF
                || isHardwareGPSDevice(context)
    }

    fun isHardwareGPSDevice(context: Context): Boolean {
        if (getLocationManager(context) == null) return false
        val providers = getLocationManager(context)!!.allProviders
        return providers.contains(LocationManager.GPS_PROVIDER)
    }

    fun showEnableGpsDialogPowerSave(activity: Activity?) {
        if (activity == null) return
        googleApiClient = GoogleApiClient.Builder(activity)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(bundle: Bundle?) {}
                override fun onConnectionSuspended(i: Int) {
                    googleApiClient.connect()
                }
            })
            .addOnConnectionFailedListener { connectionResult: ConnectionResult? -> }.build()
        googleApiClient.connect()

//            LocationRe
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        locationRequest.interval = (10 * 1000).toLong()
        locationRequest.fastestInterval = 10000
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)
        val result =
            LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build())
        result.setResultCallback { result1: LocationSettingsResult ->
            val status = result1.status
            when (status.statusCode) {
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in oncontextResult().
                    status.startResolutionForResult(activity, REQUEST_LOCATION)
                } catch (e: SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    fun showEnableGpsDialogHigh(activity: Activity?) {
        if (activity == null) return
        googleApiClient = GoogleApiClient.Builder(activity)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(bundle: Bundle?) {}
                override fun onConnectionSuspended(i: Int) {
                    googleApiClient.connect()
                }
            })
            .addOnConnectionFailedListener { connectionResult: ConnectionResult? -> }.build()
        googleApiClient.connect()

//            LocationRe
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = (10 * 1000).toLong()
        locationRequest.fastestInterval = 1000
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)
        val result =
            LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build())
        result.setResultCallback { result1: LocationSettingsResult ->
            val status = result1.status
            when (status.statusCode) {
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in oncontextResult().
                    status.startResolutionForResult(activity, REQUEST_LOCATION)
                } catch (e: SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun getLocationMode(context: Context?): Int {
        if (context == null) return Settings.Secure.LOCATION_MODE_OFF
        var locationMode = 0
        val locationProviders: String
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode =
                    Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE)
            } catch (e: SettingNotFoundException) {
                e.printStackTrace()
            }
        } else {
            locationProviders = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.LOCATION_PROVIDERS_ALLOWED
            )
            if (TextUtils.isEmpty(locationProviders)) {
                locationMode = Settings.Secure.LOCATION_MODE_OFF
            } else if (locationProviders.contains(LocationManager.GPS_PROVIDER) && locationProviders.contains(
                    LocationManager.NETWORK_PROVIDER
                )
            ) {
                locationMode = Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
            } else if (locationProviders.contains(LocationManager.GPS_PROVIDER)) {
                locationMode = Settings.Secure.LOCATION_MODE_SENSORS_ONLY
            } else if (locationProviders.contains(LocationManager.NETWORK_PROVIDER)) {
                locationMode = Settings.Secure.LOCATION_MODE_BATTERY_SAVING
            }
        }
        return locationMode
    }
}