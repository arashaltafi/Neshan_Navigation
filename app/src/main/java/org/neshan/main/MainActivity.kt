package org.neshan.main

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.carto.core.ScreenBounds
import com.carto.core.ScreenPos
import com.carto.graphics.Color
import com.carto.styles.*
import com.carto.utils.BitmapUtils
import com.google.android.gms.location.LocationRequest
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import org.neshan.R
import org.neshan.choose_location.ChooseLocationActivity
import org.neshan.common.model.LatLng
import org.neshan.common.model.LatLngBounds
import org.neshan.component.location.BoundLocationManager
import org.neshan.component.location.LocationListener
import org.neshan.component.util.showError
import org.neshan.component.util.toBitmap
import org.neshan.data.model.CircleModel
import org.neshan.data.model.error.GeneralError
import org.neshan.data.model.response.AddressDetailResponse
import org.neshan.data.network.Result
import org.neshan.data.util.EventObserver
import org.neshan.databinding.ActivityMainBinding
import org.neshan.mapsdk.model.Circle
import org.neshan.mapsdk.model.Marker
import org.neshan.mapsdk.model.Polyline
import org.neshan.mapsdk.style.NeshanMapStyle
import org.neshan.utils.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), LocationListener {

    private val mBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val bottomSheet by lazy {
        LocationDetailBottomSheet()
    }

    private var circle: Circle? = null

    private var mViewModel: MainViewModel? = null

    // handle location updates
    private var mLocationManager: BoundLocationManager? = null

    // a marker for user location to be shown on map
    private var mUserLocationMarker: Marker? = null

    // a marker for selected location to be shown on map
    private var mDestinationMarker: Marker? = null

    // poly line for the path from start point to end point on map
    private var mRoutingPathPolyLine: Polyline? = null

    private var isSuccessDetect: Boolean? = null

    private val latLngCircle: ArrayList<CircleModel> = arrayListOf()

    private var originLocation: Location? = null

    private lateinit var trafficAdapter: TrafficAdapter

    // observing choose location results
    private val mStartChooseLocationForResult = this.registerForActivityResult(
        StartActivityForResult()
    ) { result: ActivityResult ->

        // check if location selected
        if (result.resultCode == RESULT_OK && result.data != null) {
            // handle selected location
            val extras = result.data!!.extras
            val latitude =
                extras!!.getDouble(ChooseLocationActivity.EXTRA_LATITUDE)
            val longitude =
                extras.getDouble(ChooseLocationActivity.EXTRA_LONGITUDE)
            onDestinationSelected(LatLng(latitude, longitude))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        mViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        observeViewModelChange(mViewModel)
        setViewListeners()
        setUpLocationManager()

        latLngCircle.add(
            CircleModel(
                "میدان آزادی",
                LatLng(35.699005714533264, 51.33638524905276),
                500.0
            )
        )
        latLngCircle.add(
            CircleModel(
                "میدان انقلاب",
                LatLng(35.70098380940479, 51.39117847652718),
                100.0
            )
        )
        latLngCircle.add(
            CircleModel(
                "میدان ولیعصر",
                LatLng(35.71183652796547, 51.40704216281115),
                200.0
            )
        )
        latLngCircle.add(
            CircleModel(
                "نهاد رهبری",
                LatLng(35.71546875429583, 51.39979835801594),
                20.0
            )
        )
        latLngCircle.forEach {
            setCircle(it)
            mBinding.mapview.addMarker(createMarker(it))
        }

        initialTrafficList()
        listenerLogin()
        listenerLogout()

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
        mBinding.rvTraffic.adapter = trafficAdapter
    }

    private fun listenerLogin() = mBinding.apply {
        sbLogin.setOnActiveListener(object : OnActiveListener {
            override fun onActive() {
                Toast.makeText(this@MainActivity, "Test Login", Toast.LENGTH_SHORT).show()
                mBinding.sbLogin.toGone()
//                mBinding.sbLogout.toShow()
            }
        })
    }

    private fun listenerLogout() = mBinding.apply {
        sbLogout.setOnActiveListener(object : OnActiveListener {
            override fun onActive() {
                Toast.makeText(this@MainActivity, "Test Logout", Toast.LENGTH_SHORT).show()
//                mBinding.sbLogin.toShow()
//                mBinding.sbLogout.toGone()
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionResult")
        if (requestCode == BoundLocationManager.REQUEST_CODE_FOREGROUND_PERMISSIONS) {
            if (grantResults.isEmpty()) {
                Log.d(TAG, "User interaction was cancelled.")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                mLocationManager!!.startLocationUpdates()
            } else {
                // Permission denied.
                // TODO : show custom snack bar
                Snackbar.make(
                    mBinding.root,
                    R.string.permission_rationale,
                    Snackbar.LENGTH_LONG
                ).setAction(R.string.settings) {
                    // Build intent that displays the App settings screen.
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.data = Uri.fromParts(
                        "package", application.packageName, null
                    )
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }.show()
            }
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
            Toast.makeText(this, "MOCK MOCK MOCK", Toast.LENGTH_SHORT).show()
        }

        if (location.accuracy > 50f)
            mBinding.lnPoorAccuracy.toShow()
        else
            mBinding.lnPoorAccuracy.toGone()

        Log.i("test123312", "onLocationChange: ${isLocationAccurate(location)}")
        if (isSuccessDetect != isLocationAccurate(location)) {
            if (isLocationAccurate(location)) {
                Toast.makeText(this, "Location is Success Detected", Toast.LENGTH_SHORT).show()
                mBinding.rlHint.toGone()
                mBinding.sbLogin.toShow()
            } else {
                Toast.makeText(this, "Location is Not Success Detect", Toast.LENGTH_SHORT).show()
                mBinding.rlHint.toShow()
                mBinding.sbLogin.toGone()
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
            mBinding.llDistance.setOnClickListener {
                focusOnLocation(this.second.latLng, true)
            }

            mBinding.tvDistance.text = "فاصله تا " + this.second.name

            val d = this.first - this.second.radiusMeter

            val dis =
                if (d > 1000) d / 1000 else d
            val disFinal = java.lang.String.format(if (d > 1000) "%.2f" else "%.0f", dis)

            mBinding.tvDistanceMeter.text =
                if (d > 1000) "$disFinal کیلومتر " else "$disFinal متر "

            mBinding.tvHint.text =
                "فاصله تا " + this.second.name + "\n" + if (d > 1000) "$disFinal کیلومتر " else "$disFinal متر "

            if (this.first.roundToInt() <= this.second.radiusMeter) {
                mBinding.llDistance.toGone()
                mBinding.sbLogin.toShow()
                mBinding.rlHint.toGone()
            } else {
                mBinding.llDistance.toShow()
                mBinding.sbLogin.toGone()
                mBinding.rlHint.toShow()
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

    private fun observeViewModelChange(viewModel: MainViewModel?) {
        viewModel!!.locationAddressDetailLiveData.observe(
            this
        ) { (status, data): Result<AddressDetailResponse?> ->
            if (status === Result.Status.SUCCESS && data != null) {

                // hide path loading view
                mBinding.loading.toGone()

                // show location detail bottom sheet
                bottomSheet.show(supportFragmentManager, "LocationDetail")
                bottomSheet.setOnDismissListener {
                    clearMapObjects()
                }
            } else if (status === Result.Status.LOADING) {

                // show path loading view
                mBinding.loading.toShow()
            } else if (status === Result.Status.ERROR) {

                // hide path loading view
                mBinding.loading.toGone()
                clearMapObjects()
            }
        }
        viewModel.routePoints.observe(
            this
        ) { routePoints: ArrayList<LatLng> ->
            showPathOnMap(
                routePoints
            )
        }
        viewModel.generalErrorLiveData.observe(this, EventObserver { error: GeneralError? ->
            // show snack bar for errors
            showError(mBinding.root, error!!)
            null
        })
    }

    // This method gets a LatLng as input and adds a marker on that position
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
        return Marker(circleModel.latLng, markSt)
    }

    private fun setCircle(circleModel: CircleModel) {
        circle =
            Circle(
                LatLng(circleModel.latLng.latitude, circleModel.latLng.longitude),
                circleModel.radiusMeter + 5, //bug neshan (circle drawer size is not standard)
                Color(255, 100, 100, 100),
                getLineStyle()
            )
        mBinding.mapview.addCircle(circle)
//        mBinding.mapview.removeCircle(circle)
    }

    @JvmName("getLineStyle1")
    private fun getLineStyle(): LineStyle {
        val lineStyleBuilder = LineStyleBuilder()
        lineStyleBuilder.color = Color(500, 100, 100, 100)
        lineStyleBuilder.width = 4f
        return lineStyleBuilder.buildStyle()
    }

    private fun setViewListeners() {
        val colorWhite = ContextCompat.getColor(
            this@MainActivity,
            R.color.colorWhite
        )
        val colorBlack = ContextCompat.getColor(
            this@MainActivity,
            R.color.colorBlack
        )

        if (isDarkTheme()) {
            mBinding.apply {
                mapview.mapStyle = NeshanMapStyle.NESHAN_NIGHT
                tvDistance.setTextColor(colorWhite)
                tvDistanceMeter.setTextColor(colorWhite)
                view.setBackgroundColor(colorWhite)
            }
        } else {
            mBinding.apply {
                mapview.mapStyle = NeshanMapStyle.NESHAN
                tvDistance.setTextColor(colorBlack)
                tvDistanceMeter.setTextColor(colorBlack)
                view.setBackgroundColor(colorBlack)
            }
        }

        mBinding.back.setOnClickListener {
            finish()
        }

        mBinding.mapview.setOnMapLongClickListener {
            onDestinationSelected(
                it
            )
        }
        mBinding.location.setOnClickListener {
            if (mViewModel!!.startPoint != null) {
                focusOnLocation(mViewModel!!.startPoint)
            } else {
                mLocationManager!!.startLocationUpdates()
            }
        }
        mBinding.chooseLocation.setOnClickListener {
            // open Choose Location Activity to choose destination location
            mStartChooseLocationForResult.launch(Intent(this, ChooseLocationActivity::class.java))
        }

        mBinding.mapview.setOnMarkerLongClickListener {
            onDestinationSelected(
                it.latLng
            )
            true
        }

//        mBinding.mapview.setOnCircleClickListener {
//            Toast.makeText(applicationContext , "test circle ClickListener" , Toast.LENGTH_SHORT).show()
//        }

//        mBinding.mapview.setOnCircleLongClickListener {
//            mBinding.mapview.performLongClick()
//        }

        mBinding.mapview.setOnMarkerClickListener {
            focusOnLocation(it.latLng, true)
        }
    }

    /**
     * remove destination marker and drawn path for direction
     */
    private fun clearMapObjects() {
        // if user closed address detail then remove location marker from map
        if (mDestinationMarker != null) {
            mBinding.mapview.removeMarker(mDestinationMarker)
            mViewModel!!.endPoint = null
        }
        // if user closed address detail then remove drawn path from map
        if (mRoutingPathPolyLine != null) {
            mBinding.mapview.removePolyline(mRoutingPathPolyLine)
        }
        focusOnLocation(mViewModel!!.startPoint)
    }

    /**
     * set up and start location service to handle location changes,
     * receive location updates
     */
    private fun setUpLocationManager() {
        val locationRequest = LocationRequest.create()
        locationRequest.interval = TimeUnit.SECONDS.toMillis(3)
        locationRequest.fastestInterval = TimeUnit.SECONDS.toMillis(1)
        locationRequest.maxWaitTime = 1
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationManager = BoundLocationManager(this, locationRequest)
        mLocationManager!!.setLocationListener(this)
        mLocationManager!!.startLocationUpdates()
    }

    /**
     * does required actions when start location has been changed
     */
    private fun onStartPointSelected(latLng: LatLng, isCachedLocation: Boolean) {

        // remove previously added marker from map and add new marker to user location
        if (mUserLocationMarker != null) {
            mBinding.mapview.removeMarker(mUserLocationMarker)
        }
        mUserLocationMarker = if (isCachedLocation) {
            createMarker(latLng, R.drawable.ic_marker_off)
        } else {
            createMarker(latLng, R.drawable.ic_marker)
        }
        mBinding.mapview.addMarker(mUserLocationMarker)
        if (mViewModel!!.startPoint == null) {
            focusOnLocation(latLng)
        }
        mViewModel!!.startPoint = latLng
    }

    /**
     * does required actions when destination location has been chosen
     */
    private fun onDestinationSelected(latLng: LatLng) {

        // remove previously added marker from map and add new marker to selected location
        if (mDestinationMarker != null) {
            mBinding.mapview.removeMarker(mDestinationMarker)
        }
        mDestinationMarker = createMarker(latLng, R.drawable.ic_location_marker)
        mBinding.mapview.addMarker(mDestinationMarker)
        focusOnLocation(latLng)

        // load address detail for selected location
        mViewModel!!.loadAddressForLocation(latLng)
        mViewModel!!.endPoint = latLng
    }

    private fun createMarker(latLng: LatLng, iconResource: Int): Marker {
        val markStCr = MarkerStyleBuilder()
        markStCr.size = 30f
        val drawable = ContextCompat.getDrawable(this, iconResource)
        if (drawable != null) {
            val markerBitmap = BitmapUtils.createBitmapFromAndroidBitmap(drawable.toBitmap())
            markStCr.bitmap = markerBitmap
        }
        return Marker(latLng, markStCr.buildStyle())
    }

    private fun focusOnLocation(latLng: LatLng?, isMarker: Boolean = false) {
        if (latLng != null) {
            lifecycleScope.launchWhenCreated {
                mBinding.mapview.setZoom(14.5f, 0.25f)
                delay(300)
                mBinding.mapview.moveCamera(latLng, 0.25f)
                delay(300)
                mBinding.mapview.setZoom(if (isMarker) 20f else 15f, 0.25f)
            }
        }
    }

    /**
     * creates path from calculated points for direction path and shows as poly line on map
     */
    private fun showPathOnMap(routePoints: ArrayList<LatLng>) {
        if (mRoutingPathPolyLine != null) {
            mBinding.mapview.removePolyline(mRoutingPathPolyLine)
        }
        mRoutingPathPolyLine = Polyline(routePoints, lineStyle)
        mBinding.mapview.addPolyline(mRoutingPathPolyLine)

        // setup map camera to show whole path
        val latLngBounds = LatLngBounds(mViewModel!!.startPoint, mViewModel!!.endPoint)
        val mapWidth: Float = mBinding.mapview.width.coerceAtMost(mBinding.mapview.height).toFloat()
        val screenBounds = ScreenBounds(
            ScreenPos(0F, 0F),
            ScreenPos(mapWidth, mapWidth)
        )
        mBinding.mapview.moveToCameraBounds(latLngBounds, screenBounds, true, 0.5f)
    }

    private val lineStyle: LineStyle
        get() {
            val lineStCr = LineStyleBuilder()
            val color = Color(ContextCompat.getColor(this, R.color.colorPrimaryDim75))
            lineStCr.color = color
            lineStCr.width = 10f
            lineStCr.stretchFactor = 0f
            return lineStCr.buildStyle()
        }

    override fun onDestroy() {
        mLocationManager!!.stopLocationUpdates()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
