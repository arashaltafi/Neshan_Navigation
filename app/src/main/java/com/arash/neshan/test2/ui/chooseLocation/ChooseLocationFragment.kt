package com.arash.neshan.test2.ui.chooseLocation

import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.arash.neshan.test2.R
import com.arash.neshan.test2.utils.location.BoundLocationManager
import com.arash.neshan.test2.utils.location.LocationListener
import com.arash.neshan.test2.utils.toBitmap
import com.arash.neshan.test2.databinding.FragmentChooseLocationBinding
import com.arash.neshan.test2.utils.Constants
import com.arash.neshan.test2.utils.setBackStackLiveData
import com.carto.styles.MarkerStyleBuilder
import com.carto.utils.BitmapUtils
import com.google.android.gms.location.LocationRequest
import dagger.hilt.android.AndroidEntryPoint
import org.neshan.common.model.LatLng
import org.neshan.mapsdk.model.Marker
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class ChooseLocationFragment : Fragment(), LocationListener {

    private val binding by lazy {
        FragmentChooseLocationBinding.inflate(layoutInflater)
    }

    private val args by navArgs<ChooseLocationFragmentArgs>()

    private var mLocationManager: BoundLocationManager? = null
    private var mUserLocationMarker: Marker? = null
    private var mUserLocation: Location? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding.mapview.mapStyle = args.mapStyle
        setViewListeners()
        setUpLocationManager()
        return binding.root
    }

    override fun onLastLocation(location: Location) {
        onLocationChange(location)
    }

    override fun onLocationChange(location: Location) {
        binding.apply {
            val latLng = LatLng(location.latitude, location.longitude)

            // remove previously added marker from map and add new marker to user location
            if (mUserLocationMarker != null) {
                mapview.removeMarker(mUserLocationMarker)
            }
            mUserLocationMarker = createMarker(latLng)
            mapview.addMarker(mUserLocationMarker)

            if (mUserLocation == null) {
                focusOnLocation(latLng)
            }

            mUserLocation = location
        }
    }

    private fun setViewListeners() = binding.apply {
        back.setOnClickListener {
            findNavController().navigateUp()
        }

        location.setOnClickListener {
            if (mUserLocation != null) {
                focusOnLocation(LatLng(mUserLocation!!.latitude, mUserLocation!!.longitude))
            } else {
                mLocationManager?.startLocationUpdates()
            }
        }

        confirm.setOnClickListener {
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
        mLocationManager = BoundLocationManager(requireActivity(), locationRequest)
        mLocationManager?.setLocationListener(this)
        mLocationManager?.startLocationUpdates()
    }

    private fun createMarker(latLng: LatLng): Marker {
        val markStCr = MarkerStyleBuilder()

        markStCr.size = 30f

        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_marker)
        if (drawable != null) {
            val markerBitmap = BitmapUtils.createBitmapFromAndroidBitmap(drawable.toBitmap())
            markStCr.bitmap = markerBitmap
        }

        return Marker(latLng, markStCr.buildStyle())
    }

    private fun focusOnLocation(loc: LatLng) = binding.apply {
        mapview.moveCamera(loc, 0.25f)
        mapview.setZoom(15f, 0.25f)
    }

    private fun chooseSelectedPosition() {
        val latLng = binding.mapview.cameraTargetPosition
        findNavController().apply {
            setBackStackLiveData(
                Constants.BACK_FROM_CHOOSE,
                Pair(latLng.latitude, latLng.longitude)
            )
        }
    }

}