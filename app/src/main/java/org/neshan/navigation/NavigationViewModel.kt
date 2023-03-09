package org.neshan.navigation

import android.animation.ValueAnimator
import android.app.Application
import android.location.Location
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.SingleObserver
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import org.neshan.common.model.LatLng
import org.neshan.common.utils.PolylineEncoding
import org.neshan.component.util.distanceFrom
import org.neshan.component.util.equalsTo
import org.neshan.component.util.getError
import org.neshan.data.model.enums.RoutingType
import org.neshan.data.model.error.GeneralError
import org.neshan.data.model.response.Leg
import org.neshan.data.model.response.RoutingResponse
import org.neshan.data.util.Event
import javax.inject.Inject
import kotlin.math.sqrt

@HiltViewModel
class NavigationViewModel @Inject constructor(
    application: Application,
    private val mModel: NavigationModel
) : AndroidViewModel(application) {

    companion object {
        private const val MAX_DISTANCE_TOLERANCE_IN_METERS = 50
        private const val MAX_BACKWARD_MOVEMENT_TOLERANCE_IN_METERS = 2
        private const val DEFAULT_AVERAGE_SPEED_FOR_CAR = 200f
    }

    val duration = ObservableField<String>()

    val distance = ObservableField<String>()

    val address = ObservableField<String>()

    private val mCompositeDisposable by lazy { CompositeDisposable() }

    private var mStartPoint: LatLng? = null

    private var mEndPoint: LatLng? = null

    private val _generalError = MutableLiveData<Event<GeneralError>>()
    val generalError: LiveData<Event<GeneralError>> by lazy { _generalError }

    private val _routingDetail = MutableLiveData<Leg>()
    val routingDetail: LiveData<Leg> by lazy { _routingDetail }

    // remained points for routing
    private val _progressPoints = MutableLiveData<ArrayList<LatLng>>()
    val progressPoints: LiveData<ArrayList<LatLng>> by lazy { _progressPoints }

    private val _reachedDestination = MutableLiveData<Boolean>()
    val reachedDestination: LiveData<Boolean> by lazy { _reachedDestination }

    private val _markerPosition = MutableLiveData<LatLng>()
    val markerPosition: LiveData<LatLng> by lazy { _markerPosition }

    private var mRoutingPoints: ArrayList<LatLng>? = null

    private var mUserLocation: Location? = null

    private val mSpeedCalculator = SpeedCalculator(DEFAULT_AVERAGE_SPEED_FOR_CAR)

    private var mLoadingDirection = false

    private var mLastReachedPointIndex = 0

    // keeps first point of routing points to avoid repetitive path updates
    private var mLastStartingPoint: LatLng? = null

    // animate marker position from start point to end point
    private var mMarkerAnimator: ValueAnimator? = null

    fun startNavigation(startPoint: LatLng, endPoint: LatLng) {

        mStartPoint = startPoint
        mEndPoint = endPoint

        loadDirection(mStartPoint!!, mEndPoint!!, RoutingType.CAR, 0)

    }

    override fun onCleared() {

        // disposes any incomplete request to avoid possible error also unnecessary network usage
        if (!mCompositeDisposable.isDisposed) {
            mCompositeDisposable.dispose()
        }

        cancelMarkerAnimation()

        super.onCleared()

    }

    /**
     * set user location and start updating movement speed and calculate
     * passed points
     * */
    fun updateUserLocation(location: Location) {

        mUserLocation = location

        mSpeedCalculator.update(LatLng(location.latitude, location.longitude))

        // if loading direction -> avoid updating progress
        if (!mRoutingPoints.isNullOrEmpty() && !mLoadingDirection) {
            calculateUserProgress(mRoutingPoints!!)
        }

    }

    /**
     * try to load direction detail from server
     */
    private fun loadDirection(
        startPoint: LatLng,
        endPoint: LatLng,
        routingType: RoutingType,
        bearing: Int
    ) {
        if (!mLoadingDirection) {
            mLoadingDirection = true
            mModel.getDirection(routingType, startPoint, endPoint, bearing)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<RoutingResponse> {

                    override fun onSubscribe(disposable: Disposable) {
                        mCompositeDisposable.add(disposable)
                    }

                    override fun onSuccess(response: RoutingResponse) {
                        mLoadingDirection = false

                        if (response.routes != null) {

                            mRoutingPoints = ArrayList()

                            try {
                                response.routes?.firstOrNull()?.legs?.firstOrNull()?.let { leg ->

                                    _routingDetail.postValue(leg)

                                    leg.steps.map { step ->
                                        mRoutingPoints!!.addAll(PolylineEncoding.decode(step.encodedPolyline))
                                    }

                                    if (mRoutingPoints!!.size >= 2) {
                                        _progressPoints.postValue(mRoutingPoints!!)
                                        _markerPosition.postValue(mRoutingPoints!!.first())
                                    }

//                                    distance.set(leg.distance.text)
//                                    duration.set(leg.duration.text)

//                                    leg.steps.forEach {
//                                        Log.i("test123321", "step: $it")
//                                    }
//                                    Log.i("test123321", "------------------")

//                                    leg.steps.firstOrNull()?.let {
//                                        address.set("بعدی: " + leg.steps[2].name + "\n" + leg.steps.firstOrNull()?.distance?.text + "دیگر" + "" + leg.steps.firstOrNull()?.instruction)
//                                    }

                                }
                            } catch (e: NullPointerException) {
                                // failure in parsing routing detail
                                e.printStackTrace()
                            }
                        }
                    }

                    override fun onError(e: Throwable) {
                        mLoadingDirection = false
                        _generalError.postValue(Event(e.getError()))
                    }

                })
        }
    }

    /**
     * calculate remained routing points
     * */
    private fun calculateUserProgress(points: ArrayList<LatLng>) {

        val currentPoint = mRoutingPoints!!.getOrNull(mLastReachedPointIndex)
        val nextPoint = mRoutingPoints!!.getOrNull(mLastReachedPointIndex + 1)
        if (currentPoint != null && nextPoint != null && mUserLocation != null) {
            val userPoint = LatLng(mUserLocation!!.latitude, mUserLocation!!.longitude)

            val currentToNextDistance = currentPoint.distanceFrom(nextPoint)[0]
            val currentToUserDistance = currentPoint.distanceFrom(userPoint)[0]
            val nextToUserDistance = nextPoint.distanceFrom(userPoint)[0]

            // check if user moved backward
            val isUserMovedBackward = nextToUserDistance > currentToNextDistance
                    && nextToUserDistance > currentToUserDistance
                    && nextToUserDistance - currentToNextDistance > MAX_BACKWARD_MOVEMENT_TOLERANCE_IN_METERS

            // check if user has gone far from route
            var isUserFarFromRoute = false
            try {
                val s = (currentToNextDistance + currentToUserDistance + nextToUserDistance) / 2

                val area =
                    sqrt(s * (s - currentToNextDistance) * (s - currentToUserDistance) * (s - nextToUserDistance))
                val userToRouteDistance = 2 * area / currentToNextDistance

                isUserFarFromRoute = userToRouteDistance > MAX_DISTANCE_TOLERANCE_IN_METERS
            } catch (e: Exception) {
                e.printStackTrace()
            }

//            val startPoint = LatLng(mUserLocation!!.latitude, mUserLocation!!.longitude)
//            loadDirection(
//                startPoint,
//                mEndPoint!!,
//                RoutingType.CAR,
//                mUserLocation!!.bearing.toInt()
//            )

            // if user has gone far from route or moved backward -> request direction again
            if (isUserFarFromRoute || isUserMovedBackward) {
                mLastReachedPointIndex = 0

                // cancel marker animation
                cancelMarkerAnimation()

                // try to recalculate path
                val startPoint = LatLng(mUserLocation!!.latitude, mUserLocation!!.longitude)
                loadDirection(
                    startPoint,
                    mEndPoint!!,
                    RoutingType.CAR,
                    mUserLocation!!.bearing.toInt()
                )

            } else if (currentToUserDistance >= currentToNextDistance || (currentToNextDistance - currentToUserDistance) < 1) {
                // user reached next point -> update progress
                mLastReachedPointIndex++

                // get all points after closest point as remained routing points
                val remainedPoints = mRoutingPoints!!.subList(mLastReachedPointIndex, points.size)

                // if no points remained -> reached destination
                if (remainedPoints.size <= 1) {
                    _reachedDestination.postValue(true)
                } else {

                    val startingPoint = remainedPoints.first()

                    // check if start point is new
                    if (mLastStartingPoint?.equalsTo(startingPoint) != true) {

                        mLastStartingPoint = startingPoint

                        _progressPoints.postValue(ArrayList(remainedPoints))

                        // start animating marker
                        startMarkerAnimation(startingPoint, remainedPoints[1])

                    }

                }

            }

        }

    }

    /**
     * animates marker position from start point to end point
     * */
    private fun startMarkerAnimation(start: LatLng, end: LatLng) {
        if (start.equalsTo(end)) {
            return
        }

        // cancel marker animation if already running
        cancelMarkerAnimation()

        // animate marker from start point to end point in calculated duration (animationDuration)
        val distance = start.distanceFrom(end).getOrNull(0) ?: 1f
        if (distance > 0) {

            val animationDuration = distance * mSpeedCalculator.getAverageSpeedRatio()

            mMarkerAnimator = ValueAnimator.ofInt(0, 100)
            mMarkerAnimator!!.duration = animationDuration.toLong()
            mMarkerAnimator!!.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {

                var lastValue = 0

                override fun onAnimationUpdate(animation: ValueAnimator) {
                    val percentageValue = (animation.animatedValue as Int)
                    if (percentageValue != lastValue) {
                        lastValue = percentageValue
                        val latitude =
                            start.latitude + ((end.latitude - start.latitude) * percentageValue / 100)
                        val longitude =
                            start.longitude + ((end.longitude - start.longitude) * percentageValue / 100)

                        _markerPosition.postValue(LatLng(latitude, longitude))
                    }
                }

            })

            mMarkerAnimator!!.start()
        } else {
            _markerPosition.postValue(end)
        }

    }

    private fun cancelMarkerAnimation() {
        if (mMarkerAnimator != null && mMarkerAnimator!!.isRunning) {
            mMarkerAnimator!!.cancel()
        }
    }

    /**
     * helper class for calculating average speed according to past 5 visited locations
     * */
    inner class SpeedCalculator(defaultSpeed: Float) {
        private var mIndex = 0
        private val mRecords =
            floatArrayOf(defaultSpeed, defaultSpeed, defaultSpeed, defaultSpeed, defaultSpeed)
        private var mLastTime: Long = 0
        private var mLastLocation: LatLng? = null

        fun getAverageSpeedRatio(): Float {
            return mRecords.average().toFloat()
        }

        fun update(latLng: LatLng) {
            val newTime = System.currentTimeMillis()

            if (mLastLocation != null) {
                // calculate time difference with previous update
                val duration = newTime - mLastTime

                // calculate traveled distance from previous update
                mLastLocation?.distanceFrom(latLng)?.getOrNull(0)?.let { distance ->
                    if (distance > 0 && duration > 0) {
                        val speed = duration / distance
                        mRecords[mIndex % mRecords.size] = speed
                        mIndex++
                    }
                }

            }

            mLastLocation = latLng
            mLastTime = newTime

        }
    }

}