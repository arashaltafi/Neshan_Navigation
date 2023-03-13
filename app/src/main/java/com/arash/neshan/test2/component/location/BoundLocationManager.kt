package com.arash.neshan.test2.component.location

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.tasks.Task

class BoundLocationManager(
    private val mContext: AppCompatActivity,
    private val mLocationRequest: LocationRequest? = null
) : LocationManager, DefaultLifecycleObserver {

    companion object {
        private const val TAG = "BoundLocationManager"

        const val REQUEST_CODE_LOCATION_SETTING = 1001
        const val REQUEST_CODE_FOREGROUND_PERMISSIONS = 1002
    }

    private var mLocationListener: LocationListener? = null

    private var mForegroundLocationServiceBound = false

    // Provides location updates for while-in-use feature.
    private var mForegroundLocationService: ForegroundLocationService? = null

    // Listens for location broadcasts from ForegroundLocationService.
    private val mForegroundBroadcastReceiver: ForegroundBroadcastReceiver by lazy {
        ForegroundBroadcastReceiver().apply {
            this.locationListener = mLocationListener
        }
    }

    // Monitors connection to the while-in-use service.
    private val mForegroundServiceConnection: ServiceConnection by lazy {
        getServiceConnection()
    }

    private val mLocationSettingTask: Task<LocationSettingsResponse> by lazy {
        getLocationSetting()
    }

    init {
        mContext.lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {

        val serviceIntent = Intent(mContext, ForegroundLocationService::class.java)
        mContext.bindService(serviceIntent, mForegroundServiceConnection, Context.BIND_AUTO_CREATE)

    }

    override fun onResume(owner: LifecycleOwner) {

        LocalBroadcastManager.getInstance(mContext).registerReceiver(
            mForegroundBroadcastReceiver,
            IntentFilter(ForegroundLocationService.ACTION_FOREGROUND_LOCATION_BROADCAST)
        )
    }

    override fun onPause(owner: LifecycleOwner) {

        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mForegroundBroadcastReceiver)

    }

    override fun onStop(owner: LifecycleOwner) {

        if (mForegroundLocationServiceBound) {
            mContext.unbindService(mForegroundServiceConnection)
        }

//        mForegroundLocationService?.unsubscribeToLocationUpdates()

    }

    override fun setLocationListener(listener: LocationListener) {
        if (mLocationListener == null) {
            mForegroundBroadcastReceiver.locationListener = listener
        }

        mLocationListener = listener
    }

    override fun startLocationUpdates() {

        if (foregroundPermissionApproved()) {
            mForegroundLocationService?.subscribeToLocationUpdates()

            checkLocationAvailability()
        } else {
            Log.d(TAG, "Request foreground permission")
            ActivityCompat.requestPermissions(
                mContext,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_FOREGROUND_PERMISSIONS
            )
        }

    }

    override fun stopLocationUpdates() {
        mForegroundLocationService?.unsubscribeToLocationUpdates()
    }

    private fun foregroundPermissionApproved(): Boolean {

        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            mContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

    }

    private fun getServiceConnection(): ServiceConnection {

        return object : ServiceConnection {

            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                val binder = service as ForegroundLocationService.LocalBinder
                mForegroundLocationService = binder.service

                mLocationRequest?.let { mForegroundLocationService?.setLocationRequest(it) }

                mForegroundLocationServiceBound = true

                startLocationUpdates()
            }

            override fun onServiceDisconnected(name: ComponentName) {
                mForegroundLocationService = null
                mForegroundLocationServiceBound = false
            }
        }

    }

    private fun getLocationSetting(): Task<LocationSettingsResponse> {

        val builder = LocationSettingsRequest.Builder()
        mForegroundLocationService?.getLocationRequest()?.let {
            builder.addLocationRequest(it)
        }

        return LocationServices.getSettingsClient(mContext).checkLocationSettings(builder.build())

    }

    private fun checkLocationAvailability() {

        mLocationSettingTask.addOnSuccessListener {
            Log.d(TAG, "All location settings are satisfied")
        }.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                Log.e(TAG, "Location settings are not satisfied")
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        mContext,
                        REQUEST_CODE_LOCATION_SETTING
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }

    }

    private inner class ForegroundBroadcastReceiver : BroadcastReceiver() {
        var locationListener: LocationListener? = null

        override fun onReceive(context: Context, intent: Intent) {

            if (intent.hasExtra(ForegroundLocationService.EXTRA_LOCATION)) {
                val location =
                    intent.getParcelableExtra<Location>(ForegroundLocationService.EXTRA_LOCATION)
                location?.let { locationListener?.onLocationChange(location) }
            } else if (intent.hasExtra(ForegroundLocationService.EXTRA_LAST_LOCATION)) {
                val location =
                    intent.getParcelableExtra<Location>(ForegroundLocationService.EXTRA_LAST_LOCATION)
                location?.let { locationListener?.onLastLocation(location) }
            }

        }
    }

}