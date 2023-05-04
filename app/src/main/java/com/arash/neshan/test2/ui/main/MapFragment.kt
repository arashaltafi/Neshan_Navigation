package com.arash.neshan.test2.ui.main

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.arash.neshan.test2.R
import com.arash.neshan.test2.utils.location.BoundLocationManager
import com.arash.neshan.test2.utils.location.LocationListener
import com.arash.neshan.test2.utils.showError
import com.arash.neshan.test2.utils.toBitmap
import com.arash.neshan.test2.domain.model.CircleModel
import com.arash.neshan.test2.databinding.FragmentMapBinding
import com.arash.neshan.test2.domain.model.response.AddressDetailResponse
import com.arash.neshan.test2.utils.*
import com.arash.neshan.test2.utils.swipe.OnActiveListener
import com.carto.core.ScreenBounds
import com.carto.core.ScreenPos
import com.carto.graphics.Color
import com.carto.styles.LineStyle
import com.carto.styles.LineStyleBuilder
import com.carto.styles.MarkerStyleBuilder
import com.carto.utils.BitmapUtils
import com.google.android.gms.location.LocationRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import org.neshan.common.model.LatLng
import org.neshan.common.model.LatLngBounds
import org.neshan.mapsdk.model.Circle
import org.neshan.mapsdk.model.Marker
import org.neshan.mapsdk.model.Polyline
import org.neshan.mapsdk.style.NeshanMapStyle
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@AndroidEntryPoint
class MapFragment : Fragment(), LocationListener {

    private val binding by lazy {
        FragmentMapBinding.inflate(layoutInflater)
    }

    private val lineStyle: LineStyle
        get() {
            val lineStCr = LineStyleBuilder()
            val color = Color(ContextCompat.getColor(requireContext(), R.color.colorPrimaryDim75))
            lineStCr.color = color
            lineStCr.width = 10f
            lineStCr.stretchFactor = 0f
            return lineStCr.buildStyle()
        }

    private var circle: Circle? = null
    private val mViewModel by viewModels<MainViewModel>()
    private var mLocationManager: BoundLocationManager? = null
    private var mUserLocationMarker: Marker? = null
    private var mDestinationMarker: Marker? = null
    private var mRoutingPathPolyLine: Polyline? = null
    private var isSuccessDetect: Boolean? = null
    private val latLngCircle: ArrayList<CircleModel> = arrayListOf()
    private var originLocation: Location? = null
    private lateinit var trafficAdapter: TrafficAdapter

    private val registerResult = PermissionUtils.register(this,
        object : PermissionUtils.PermissionListener {
            override fun observe(permissions: Map<String, Boolean>) {
                if (permissions[ACCESS_FINE_LOCATION] == true) {
                    Toast.makeText(
                        requireContext(),
                        "permission has granted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    mLocationManager!!.startLocationUpdates()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "permission has not granted!!!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBackStackObservers()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        initializeMapSetting()
        observeViewModelChange()
        setViewListeners()
        setUpLocationManager()
        initialTrafficList()
        listenerLogin()
        listenerLogout()
        getPermission()

        //check distance
        Log.i(
            "test123321",
            "distanceInMeter: ${
                distanceInMeter(
                    35.719078027015286,
                    51.396623577882245,
                    35.71842254959612,
                    51.39768305064806
                )
            }"
        )

        return binding.root
    }

    private fun initializeMapSetting() = binding.apply {
        latLngCircle.add(
            CircleModel(
                "میدان آزادی",
                "متن توضیحات میدان آزادی",
                LatLng(35.699005714533264, 51.33638524905276),
                500.0
            )
        )
        latLngCircle.add(
            CircleModel(
                "میدان انقلاب",
                "متن توضیحات میدان انقلاب",
                LatLng(35.70098380940479, 51.39117847652718),
                100.0
            )
        )
        latLngCircle.add(
            CircleModel(
                "میدان ولیعصر",
                "متن توضیحات میدان ولیعصر",
                LatLng(35.71183652796547, 51.40704216281115),
                200.0
            )
        )

        latLngCircle.forEach {
            setCircle(it)
            mapview.addMarker(createMarker(it))
        }

        mapview.isPoiEnabled = true
        mapview.isTrafficEnabled = true

//        val latLngBounds = LatLngBounds(
//            LatLng(41.95570991266906, 50.60572931619605),
//            LatLng(24.062751970294773, 55.76843173696899)
//        )
//        val screenBounds = ScreenBounds()
//        mapview.moveToCameraBounds(latLngBounds, screenBounds, true, 1f)
    }

    private fun initialTrafficList() {
        val timeList = arrayListOf(
            getString(R.string.logout_roll_call),
            getString(R.string.login_roll_call),
            getString(R.string.empty_time),
            getString(R.string.empty_time),
            getString(R.string.empty_time),
            getString(R.string.empty_time),
            getString(R.string.empty_time),
            getString(R.string.empty_time),
            getString(R.string.empty_time),
            getString(R.string.empty_time)
        )
        trafficAdapter = TrafficAdapter(timeList)
        binding.rvTraffic.adapter = trafficAdapter
    }

    private fun listenerLogin() = binding.apply {
        sbLogin.setOnActiveListener(object : OnActiveListener {
            override fun onActive() {
                Toast.makeText(requireContext(), "Test Login", Toast.LENGTH_SHORT).show()
                binding.sbLogin.toGone()
            }
        })
    }

    private fun listenerLogout() = binding.apply {
        sbLogout.setOnActiveListener(object : OnActiveListener {
            override fun onActive() {
                Toast.makeText(requireContext(), "Test Logout", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getPermission() {
        if (PermissionUtils.isGranted(requireContext(), ACCESS_FINE_LOCATION).not()) {
            PermissionUtils.requestPermission(
                requireContext(), registerResult, ACCESS_FINE_LOCATION
            )
        } else {
            mLocationManager!!.startLocationUpdates()
        }
    }

    override fun onLastLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        onStartPointSelected(latLng, true)
        originLocation = location
    }

    @SuppressLint("SetTextI18n")
    override fun onLocationChange(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        onStartPointSelected(latLng, false)

        if (location.hasMock()) {
            Toast.makeText(requireContext(), "MOCK MOCK MOCK", Toast.LENGTH_SHORT).show()
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

            if (location.accuracy > 50f)
                lnPoorAccuracy.toShow()
            else
                lnPoorAccuracy.toGone()

            Log.i("test123312", "onLocationChange: ${isLocationAccurate(location)}")
            if (isSuccessDetect != isLocationAccurate(location)) {
                if (isLocationAccurate(location)) {
                    Toast.makeText(
                        requireContext(),
                        "Location is Success Detected",
                        Toast.LENGTH_SHORT
                    ).show()
                    rlHint.toGone()
                    sbLogin.toShow()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Location is Not Success Detect",
                        Toast.LENGTH_SHORT
                    ).show()
                    rlHint.toShow()
                    sbLogin.toGone()
                }
            }
            isSuccessDetect = isLocationAccurate(location)

            val distanceList: ArrayList<Pair<Float, CircleModel>> = arrayListOf()
            latLngCircle.forEach {
                distanceList.add(
                    Pair(
                        distanceInMeter(
                            location.latitude,
                            location.longitude,
                            it.latLng.latitude,
                            it.latLng.longitude
                        ),
                        it
                    )
                )
            }

            distanceList.minByOrNull {
                it.first
            }?.apply {
                llDistance.setOnClickListener {
//                focusOnLocation(this.second.latLng, true)
                }

                view.toShow()

                tvDistance.text = "فاصله تا " + this.second.title

                val d = this.first - this.second.radiusMeter

                val dis =
                    if (d > 1000) d / 1000 else d
                val disFinal = java.lang.String.format(if (d > 1000) "%.2f" else "%.0f", dis)

                tvDistanceMeter.text =
                    if (d > 1000) "$disFinal کیلومتر " else "$disFinal متر "

                tvHint.text =
                    "فاصله تا " + this.second.title + "\n" + if (d > 1000) "$disFinal کیلومتر " else "$disFinal متر "

                if (this.first.roundToInt() <= this.second.radiusMeter) {
                    llDistance.toGone()
                    sbLogin.toShow()
                    rlHint.toGone()
                } else {
                    llDistance.toShow()
                    sbLogin.toGone()
                    rlHint.toShow()
                }
            }
        }
    }

    private fun isLocationAccurate(location: Location): Boolean {
        // Check if location accuracy is less than 100 meters
        // and if the location is less than 5 minutes old
        val accuracy = location.accuracy
        val time = System.currentTimeMillis() - location.time
        return accuracy < 100.0f && time < 5 * 60 * 1000
    }

    private fun observeViewModelChange() {
        var addressDetailResponse: AddressDetailResponse? = null

        mViewModel.liveLocationAddressDetail.observe(viewLifecycleOwner) {
            binding.apply {
                loading.toGone()
                addressDetailResponse = it
            }
        }

        mViewModel.liveRoutePoints.observe(viewLifecycleOwner) {
            findNavController().navigate(
                R.id.locationDetailBottomSheet,
                LocationDetailBottomSheetArgs(addressDetailResponse, it).toBundle()
            )
        }

        mViewModel.routePoints.observe(viewLifecycleOwner) { routePoints: ArrayList<LatLng> ->
            showPathOnMap(routePoints)
        }

        mViewModel.liveGeneralError.observe(viewLifecycleOwner) {
            showError(binding.root, it)
        }
    }

    private fun createMarker(circleModel: CircleModel): Marker {
        val markStCr = MarkerStyleBuilder()
        markStCr.size = 30f
        markStCr.bitmap =
            org.neshan.mapsdk.internal.utils.BitmapUtils.createBitmapFromAndroidBitmap(
                BitmapFactory.decodeResource(
                    resources, R.drawable.ic_marker_pin
                )
            )
        val markSt = markStCr.buildStyle()

        val marker = Marker(circleModel.latLng, markSt)
        marker.title = circleModel.title
        marker.description = circleModel.description
        return marker
    }

    private fun setCircle(circleModel: CircleModel) = binding.apply {
        circle =
            Circle(
                LatLng(circleModel.latLng.latitude, circleModel.latLng.longitude),
                circleModel.radiusMeter + 5, //bug neshan (circle drawer size is not standard)
                Color(255, 100, 100, 100),
                getLineStyle()
            )
        mapview.addCircle(circle)
//        mapview.removeCircle(circle)
    }

    @JvmName("getLineStyle1")
    private fun getLineStyle(): LineStyle {
        val lineStyleBuilder = LineStyleBuilder()
        lineStyleBuilder.color = Color(500, 100, 100, 100)
        lineStyleBuilder.width = 4f
        return lineStyleBuilder.buildStyle()
    }

    private fun setViewListeners() = binding.apply {
        val colorWhite = ContextCompat.getColor(
            requireContext(),
            R.color.colorWhite
        )
        val colorBlack = ContextCompat.getColor(
            requireContext(),
            R.color.colorBlack
        )

        back.setOnClickListener {
            findNavController().navigateUp()
        }

        mapview.setOnMapLongClickListener {
            onDestinationSelected(it)
        }

        /*if (mBinding.mapview.mapStyle == NeshanMapStyle.NESHAN) {
            mBinding.theme.setImageResource(R.drawable.ic_baseline_nights_stay_24)
        } else {
            mBinding.theme.setImageResource(R.drawable.ic_baseline_light_mode_24)
        }*/

        theme.setOnClickListener {
            if (mapview.mapStyle == NeshanMapStyle.NESHAN) {
                changeIconTheme(true)
                mapview.mapStyle = NeshanMapStyle.NESHAN_NIGHT
                tvDistance.setTextColor(colorWhite)
                tvDistanceMeter.setTextColor(colorWhite)
                view.setBackgroundColor(colorWhite)

            } else {
                changeIconTheme(false)
                mapview.mapStyle = NeshanMapStyle.NESHAN
                tvDistance.setTextColor(colorBlack)
                tvDistanceMeter.setTextColor(colorBlack)
                view.setBackgroundColor(colorBlack)

            }
        }

        location.setOnClickListener {
            if (isEnableGps().not())
                GpsUtils.enableGps(requireActivity())
            else {
                if (mViewModel.startPoint != null) {
                    focusOnLocation(mViewModel.startPoint)
                } else {
                    mLocationManager!!.startLocationUpdates()
                }
            }
        }

        chooseLocation.setOnClickListener {
            findNavController().navigate(
                MapFragmentDirections.actionMapFragmentToChooseLocationFragment(
                    mapview.mapStyle
                )
            )
        }

        mapview.setOnMarkerLongClickListener {
            onDestinationSelected(it.latLng)
        }

//        mapview.setOnCircleClickListener {
//            Toast.makeText(requireContext(), "test circle ClickListener", Toast.LENGTH_SHORT).show()
//        }
//        mapview.setOnCircleDoubleClickListener {
//
//        }

        mapview.setOnCircleLongClickListener {
            mapview.performLongClick()
        }

        mapview.setOnMarkerClickListener {
            it.showInfoWindow()
//            focusOnLocation(it.latLng, true)
//            openChooseNavigation(it.latLng.latitude, it.latLng.longitude)
//            openGoogleMapNavigation(it.latLng.latitude, it.latLng.longitude)
//            openNeshanNavigation(it.latLng.latitude, it.latLng.longitude)
        }
    }

    private fun changeIconTheme(isNightMode: Boolean) = binding.apply {
        val icon = if (isNightMode) R.drawable.ic_baseline_light_mode_24
        else R.drawable.ic_baseline_nights_stay_24
        theme.setImageResource(icon)
    }

    private fun isEnableGps(): Boolean =
        GpsUtils.isDisabledGps(requireContext()).not()

    private fun clearMapObjects() = binding.apply {
        // if user closed address detail then remove location marker from map
        if (mDestinationMarker != null) {
            mapview.removeMarker(mDestinationMarker)
            mViewModel.endPoint = null
        }
        // if user closed address detail then remove drawn path from map
        if (mRoutingPathPolyLine != null) {
            mapview.removePolyline(mRoutingPathPolyLine)
        }
        focusOnLocation(mViewModel.startPoint)
    }

    private fun setUpLocationManager() {
        val locationRequest = LocationRequest.create()
        locationRequest.interval = TimeUnit.SECONDS.toMillis(3)
        locationRequest.fastestInterval = TimeUnit.SECONDS.toMillis(1)
        locationRequest.maxWaitTime = 1
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationManager = BoundLocationManager(requireActivity(), locationRequest)
        mLocationManager!!.setLocationListener(this)
        mLocationManager!!.startLocationUpdates()
    }

    private fun onStartPointSelected(latLng: LatLng, isCachedLocation: Boolean) = binding.apply {
        if (mUserLocationMarker != null) {
            mapview.removeMarker(mUserLocationMarker)
        }
        mUserLocationMarker = if (isCachedLocation) {
            createMarker(latLng, R.drawable.ic_marker_off)
        } else {
            createMarker(latLng, R.drawable.ic_marker)
        }
        mapview.addMarker(mUserLocationMarker)
        if (mViewModel.startPoint == null) {
            focusOnLocation(latLng)
        }
        mViewModel.startPoint = latLng
    }

    private fun onDestinationSelected(latLng: LatLng) = binding.apply {
        if (mDestinationMarker != null) {
            mapview.removeMarker(mDestinationMarker)
        }
        mDestinationMarker = createMarker(latLng, R.drawable.ic_location_marker)
        mapview.addMarker(mDestinationMarker)
        focusOnLocation(latLng)

        // load address detail for selected location
        mViewModel.loadAddressForLocation(latLng)
        mViewModel.endPoint = latLng
    }

    private fun createMarker(latLng: LatLng, iconResource: Int): Marker {
        val markStCr = MarkerStyleBuilder()
        markStCr.size = 30f
        val drawable = ContextCompat.getDrawable(requireContext(), iconResource)
        if (drawable != null) {
            val markerBitmap = BitmapUtils.createBitmapFromAndroidBitmap(drawable.toBitmap())
            markStCr.bitmap = markerBitmap
        }
        return Marker(latLng, markStCr.buildStyle())
    }

    private fun focusOnLocation(latLng: LatLng?, isMarker: Boolean = false) = binding.apply {
        if (latLng != null) {
            lifecycleScope.launchWhenCreated {
                mapview.setZoom(14.5f, 0.25f)
                delay(300)
                mapview.moveCamera(latLng, 0.25f)
                delay(300)
                mapview.setZoom(if (isMarker) 20f else 15f, 0.25f)
            }
        }
    }

    private fun showPathOnMap(routePoints: ArrayList<LatLng>) = binding.apply {
        if (mRoutingPathPolyLine != null) {
            mapview.removePolyline(mRoutingPathPolyLine)
        }
        mRoutingPathPolyLine = Polyline(routePoints, lineStyle)
        mapview.addPolyline(mRoutingPathPolyLine)

        // setup map camera to show whole path
        val latLngBounds = LatLngBounds(mViewModel.startPoint, mViewModel.endPoint)
        val mapWidth: Float = mapview.width.coerceAtMost(mapview.height).toFloat()
        val screenBounds = ScreenBounds(
            ScreenPos(0F, 0F),
            ScreenPos(mapWidth, mapWidth)
        )
        mapview.moveToCameraBounds(latLngBounds, screenBounds, true, 0.5f)
    }

    private fun initBackStackObservers() {
        findNavController().getBackStackLiveData<Pair<Double, Double>>(Constants.BACK_FROM_CHOOSE)
            ?.observe(this) {
                val latLng = LatLng(it.first, it.second)
                onDestinationSelected(latLng)

                requireActivity().intent = Intent() // clear intent
            }

        findNavController().getBackStackLiveData<Any>(Constants.BACK_FROM_BOTTOM_SHEET)
            ?.observe(this) {
                if (it.cast<Boolean>() == true)
                    clearMapObjects()
                else {
                    var startLatLng: com.arash.neshan.test2.domain.model.LatLng? = null
                    mViewModel.startPoint?.latitude?.let { startLatitude ->
                        mViewModel.startPoint?.longitude?.let { startLongitude ->
                            startLatLng = com.arash.neshan.test2.domain.model.LatLng(
                                startLatitude, startLongitude
                            )
                        }
                    }

                    var endLatLng: com.arash.neshan.test2.domain.model.LatLng? = null
                    mViewModel.endPoint?.latitude?.let { endLatitude ->
                        mViewModel.endPoint?.longitude?.let { endLongitude ->
                            endLatLng = com.arash.neshan.test2.domain.model.LatLng(
                                endLatitude, endLongitude
                            )
                        }
                    }

                    if (startLatLng != null && endLatLng != null) {
                        runAfter(500, {
                            goToNavigation(startLatLng!!, endLatLng!!, binding.mapview.mapStyle)
                        })
                    }

                    requireActivity().intent = Intent() // clear intent
                }
            }
    }

    private fun goToNavigation(
        startLatLng: com.arash.neshan.test2.domain.model.LatLng,
        endLatLng: com.arash.neshan.test2.domain.model.LatLng,
        mapStyle: Int
    ) {
        findNavController().navigate(
            MapFragmentDirections.actionMapFragmentToNavigationFragment(
                startLatLng,
                endLatLng,
                mapStyle
            )
        )
    }

    override fun onDestroy() {
        mLocationManager!!.stopLocationUpdates()
        super.onDestroy()
    }
}