package com.arash.neshan.test2.ui.navigation

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.arash.neshan.test2.R
import com.arash.neshan.test2.utils.location.BoundLocationManager
import com.arash.neshan.test2.utils.location.FakeLocationManager
import com.arash.neshan.test2.utils.location.LocationListener
import com.arash.neshan.test2.utils.location.LocationManager
import com.arash.neshan.test2.utils.angleWithNorthAxis
import com.arash.neshan.test2.utils.showError
import com.arash.neshan.test2.utils.toBitmap
import com.arash.neshan.test2.utils.snackbar.SnackBar
import com.arash.neshan.test2.domain.model.error.GeneralError
import com.arash.neshan.test2.domain.model.error.SimpleError
import com.arash.neshan.test2.domain.model.response.Step
import com.arash.neshan.test2.domain.model.util.EventObserver
import com.arash.neshan.test2.databinding.FragmentNavigationBinding
import com.arash.neshan.test2.utils.distanceInMeter
import com.arash.neshan.test2.utils.toGone
import com.arash.neshan.test2.utils.toShow
import com.carto.graphics.Color
import com.carto.styles.LineStyle
import com.carto.styles.LineStyleBuilder
import com.carto.styles.MarkerStyleBuilder
import com.carto.utils.BitmapUtils
import com.google.android.gms.location.LocationRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.neshan.common.model.LatLng
import org.neshan.mapsdk.model.Marker
import org.neshan.mapsdk.model.Polyline
import kotlin.math.roundToInt

@AndroidEntryPoint
class NavigationFragment : Fragment(), LocationListener {

    private val binding by lazy {
        FragmentNavigationBinding.inflate(layoutInflater)
    }

    private val args by navArgs<NavigationFragmentArgs>()

    private val mViewModel by viewModels<NavigationViewModel>()

    private var mLocationManager: LocationManager? = null
    private var mUserLocationMarker: Marker? = null
    private var mProgressPathPolyLine: Polyline? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initializeMapSetting()
        setViewListeners()
        observeViewModelChange(mViewModel)
        setUpLocationManager()
        loadNavigationData()

        return binding.root
    }

    private fun initializeMapSetting() = binding.apply {
        mapview.mapStyle = args.mapStyle
        mapview.isTrafficEnabled = true
        mapview.isPoiEnabled = true
        mapview.setTilt(40f, 0.25f)
    }

    override fun onLastLocation(location: Location) {
        onLocationChange(location)
    }

    @SuppressLint("SetTextI18n")
    override fun onLocationChange(location: Location) {
        val distanceList: ArrayList<Pair<Float, Step>> = arrayListOf()
        mViewModel.updateUserLocation(location)
        mViewModel.routingDetail.observe(this) {
            for (i in it.steps) {
                distanceList.add(
                    Pair(
                        distanceInMeter(
                            location.latitude,
                            location.longitude,
                            i.startLocation.last(),
                            i.startLocation.first()
                        ),
                        i
                    )
                )
            }

            binding.apply {
                val metersPerSecond = location.speed.roundToInt()
                val kilometersPerHour = metersPerSecond * 3.6
                if (kilometersPerHour > 0)
                    cvSpeed.toShow()
                else
                    cvSpeed.toGone()

                tvSpeed.text = kilometersPerHour.roundToInt().toString()

                Log.i("test123321", "kilometersPerHour: $kilometersPerHour")

                distance.text = it.distance.text
                duration.text = it.duration.text

                distanceList.minByOrNull { pair ->
                    pair.first
                }?.second?.apply {
                    address.text =
                        "بعدی: " + name + "\n" + distance.text + "دیگر" + "" + instruction
                }
            }
        }
    }

    private fun setViewListeners() = binding.apply {
        stop.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadNavigationData() {
        val startPoint = args.startPoint
        val endPoint = args.endPoint

        if (startPoint != null && endPoint != null) {
            mViewModel.startNavigation(
                LatLng(startPoint.latitude, startPoint.longitude),
                LatLng(endPoint.latitude, endPoint.longitude)
            )
        } else {
            showError(binding.root, SimpleError(R.string.navigation_failure))
            findNavController().navigateUp()
        }
    }

    private fun observeViewModelChange(viewModel: NavigationViewModel) {
        if (USE_FAKE_LOCATIONS) {
            viewModel.routingDetail.observe(viewLifecycleOwner) { routingDetail ->
                mLocationManager = FakeLocationManager(routingDetail)
                mLocationManager?.setLocationListener(this)
                mLocationManager?.startLocationUpdates()
            }
        }

        viewModel.progressPoints.observe(viewLifecycleOwner) { progressPoints ->
            updatePathOnMap(progressPoints)
        }

        viewModel.markerPosition.observe(viewLifecycleOwner) { markerPosition ->
            updateLocationMarker(markerPosition)
        }

        viewModel.reachedDestination.observe(viewLifecycleOwner) { reachedDestination ->
            if (reachedDestination) {
                SnackBar.make(binding.root, getString(R.string.reached_destination)).show()
                lifecycleScope.launch {
                    delay(3000)
                    findNavController().navigateUp()
                }
            }
        }

        viewModel.generalError.observe(viewLifecycleOwner, EventObserver { error: GeneralError ->
            showError(binding.root, error)
        })
    }

    private fun updatePathOnMap(routePoints: ArrayList<LatLng>) = binding.apply {
        if (routePoints.size >= 2) {
            // create new poly line by routing points and update path on map
            if (mProgressPathPolyLine != null) {
                mapview.removePolyline(mProgressPathPolyLine)
            }
            mProgressPathPolyLine =
                Polyline(routePoints, getLineStyle(R.color.colorPrimaryDim75))
            mapview.addPolyline(mProgressPathPolyLine)

            // calculate first route angle with north axis
            // and set camera rotation to always show upward
            val startPoint = routePoints[0]
            val endPoint = routePoints[1]
            val bearingEndPoint = routePoints.getOrNull(2) ?: endPoint
            val angle = angleWithNorthAxis(startPoint, bearingEndPoint)
            mapview.setBearing((angle).toFloat(), 0.7f)

            focusOnLocation(startPoint)
        }
    }

    private fun getLineStyle(colorResource: Int): LineStyle? {
        val lineStCr = LineStyleBuilder().apply {
            color = Color(ContextCompat.getColor(requireContext(), colorResource))
            width = 10f
            stretchFactor = 0f
        }
        return lineStCr.buildStyle()
    }

    private fun setUpLocationManager() {
        val locationRequest = LocationRequest.create().apply {
            interval = LOCATION_UPDATE_INTERVAL
            fastestInterval = LOCATION_UPDATE_FASTEST_INTERVAL
            maxWaitTime = 1
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (!USE_FAKE_LOCATIONS) {
            mLocationManager = BoundLocationManager(requireActivity(), locationRequest)
            mLocationManager?.setLocationListener(this)
            mLocationManager?.startLocationUpdates()
        }
    }

    private fun updateLocationMarker(latLng: LatLng) = binding.apply {
        if (mUserLocationMarker != null) {
            mapview.removeMarker(mUserLocationMarker)
        }

        mUserLocationMarker = createMarker(latLng)

        mapview.addMarker(mUserLocationMarker)
    }

    private fun createMarker(
        latLng: LatLng,
        marker: Int = R.drawable.ic_baseline_navigation_24
    ): Marker {
        val markStCr = MarkerStyleBuilder()

        markStCr.size = 30f

        val drawable = ContextCompat.getDrawable(requireContext(), marker)
        if (drawable != null) {
            val markerBitmap = BitmapUtils.createBitmapFromAndroidBitmap(drawable.toBitmap())
            markStCr.bitmap = markerBitmap
        }

        return Marker(latLng, markStCr.buildStyle())
    }

    private fun focusOnLocation(loc: LatLng) = binding.apply {
        mapview.moveCamera(loc, 0.5f)
        if (mapview.zoom != 18f) {
            mapview.setZoom(18f, 0.5f)
        }
    }

    companion object {
        private const val USE_FAKE_LOCATIONS = false
        private const val LOCATION_UPDATE_INTERVAL = 3000L // 3 seconds
        private const val LOCATION_UPDATE_FASTEST_INTERVAL = 1000L // 1 second
    }

}