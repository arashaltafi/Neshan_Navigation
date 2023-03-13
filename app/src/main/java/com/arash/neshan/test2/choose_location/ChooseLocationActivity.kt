package com.arash.neshan.test2.choose_location

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.arash.neshan.test2.R
import com.arash.neshan.test2.component.location.BoundLocationManager
import com.arash.neshan.test2.component.location.BoundLocationManager.Companion.REQUEST_CODE_FOREGROUND_PERMISSIONS
import com.arash.neshan.test2.component.location.LocationListener
import com.arash.neshan.test2.component.util.toBitmap
import com.arash.neshan.test2.databinding.ActivityChooseLocationBinding
import com.carto.styles.MarkerStyleBuilder
import com.carto.utils.BitmapUtils
import com.google.android.gms.location.LocationRequest
import com.google.android.material.snackbar.Snackbar
import org.neshan.common.model.LatLng
import org.neshan.mapsdk.model.Marker
import java.util.concurrent.TimeUnit

class ChooseLocationActivity : AppCompatActivity(), LocationListener {

    companion object {
        private const val TAG = "ChooseLocationActivity"

        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
    }

    private lateinit var mBinding: ActivityChooseLocationBinding

    // handle location updates
    private var mLocationManager: BoundLocationManager? = null

    // a marker for user location to be shown on map
    private var mUserLocationMarker: Marker? = null

    private var mUserLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityChooseLocationBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setViewListeners()

        setUpLocationManager()

        intent.getIntExtra("MAP_STYLE", 1).let {
            mBinding.mapview.mapStyle = it
        }
    }

    override fun onLastLocation(location: Location) {
        onLocationChange(location)
    }

    // handle location change
    override fun onLocationChange(location: Location) {

        val latLng = LatLng(location.latitude, location.longitude)

        // remove previously added marker from map and add new marker to user location
        if (mUserLocationMarker != null) {
            mBinding.mapview.removeMarker(mUserLocationMarker)
        }
        mUserLocationMarker = createMarker(latLng)
        mBinding.mapview.addMarker(mUserLocationMarker)

        if (mUserLocation == null) {
            focusOnLocation(latLng)
        }

        mUserLocation = location

    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        Log.d(TAG, "onRequestPermissionResult")

        when (requestCode) {
            REQUEST_CODE_FOREGROUND_PERMISSIONS -> when {
                grantResults.isEmpty() ->
                    // If user interaction was interrupted, the permission request
                    // is cancelled and you receive empty arrays.
                    Log.d(TAG, "User interaction was cancelled.")
                grantResults[0] == PackageManager.PERMISSION_GRANTED ->
                    // Permission was granted.
                    mLocationManager?.startLocationUpdates()
                else -> {
                    // Permission denied.

                    Snackbar.make(
                        mBinding.root,
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_LONG
                    )
                        .setAction(R.string.settings) {
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts(
                                "package",
                                application.packageName,
                                null
                            )
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }.show()
                }
            }
        }

    }

    private fun setViewListeners() {

        mBinding.back.setOnClickListener {
            onBackPressed()
        }

        mBinding.location.setOnClickListener {
            if (mUserLocation != null) {
                focusOnLocation(LatLng(mUserLocation!!.latitude, mUserLocation!!.longitude))
            } else {
                mLocationManager?.startLocationUpdates()
            }
        }

        mBinding.confirm.setOnClickListener {
            chooseSelectedPosition()
        }

    }

    private fun setUpLocationManager() {
        val locationRequest = LocationRequest.create().apply {
            interval = TimeUnit.SECONDS.toMillis(3)
            fastestInterval = TimeUnit.SECONDS.toMillis(1)
            maxWaitTime = 1
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        mLocationManager = BoundLocationManager(this, locationRequest)
        mLocationManager?.setLocationListener(this)
        mLocationManager?.startLocationUpdates()
    }

    private fun createMarker(latLng: LatLng): Marker {

        val markStCr = MarkerStyleBuilder()

        markStCr.size = 30f

        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_marker)
        if (drawable != null) {
            val markerBitmap = BitmapUtils.createBitmapFromAndroidBitmap(drawable.toBitmap())
            markStCr.bitmap = markerBitmap
        }

        return Marker(latLng, markStCr.buildStyle())

    }

    private fun focusOnLocation(loc: LatLng) {

        mBinding.mapview.moveCamera(loc, 0.25f)
        mBinding.mapview.setZoom(15f, 0.25f)

    }

    private fun chooseSelectedPosition() {

        val latLng = mBinding.mapview.cameraTargetPosition
        setResult(RESULT_OK, Intent().apply {
            putExtra(EXTRA_LATITUDE, latLng.latitude)
            putExtra(EXTRA_LONGITUDE, latLng.longitude)
        })

        onBackPressed()

    }

}