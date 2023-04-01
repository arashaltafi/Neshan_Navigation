package com.arash.neshan.test2.ui.navigation

import android.animation.ValueAnimator
import android.location.Location
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arash.neshan.test2.domain.model.enums.RoutingType
import com.arash.neshan.test2.domain.model.error.GeneralError
import com.arash.neshan.test2.domain.model.response.Leg
import com.arash.neshan.test2.domain.model.util.Event
import com.arash.neshan.test2.domain.repository.NavigationRepository
import com.arash.neshan.test2.utils.base.BaseViewModel
import com.arash.neshan.test2.utils.distanceFrom
import com.arash.neshan.test2.utils.equalsTo
import com.arash.neshan.test2.utils.getError
import dagger.hilt.android.lifecycle.HiltViewModel
import org.neshan.common.model.LatLng
import org.neshan.common.utils.PolylineEncoding
import javax.inject.Inject
import kotlin.math.sqrt

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val navigationRepository: NavigationRepository
) : BaseViewModel() {

    val duration = ObservableField<String>()

    val distance = ObservableField<String>()

    val address = ObservableField<String>()

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

    private var mLastStartingPoint: LatLng? = null

    private var mMarkerAnimator: ValueAnimator? = null

    fun startNavigation(startPoint: LatLng, endPoint: LatLng) {
        mStartPoint = startPoint
        mEndPoint = endPoint
        loadDirection(mStartPoint!!, mEndPoint!!, RoutingType.CAR, 0)
    }

    fun updateUserLocation(location: Location) {
        mUserLocation = location
        mSpeedCalculator.update(LatLng(location.latitude, location.longitude))
        // if loading direction -> avoid updating progress
        if (!mRoutingPoints.isNullOrEmpty() && !mLoadingDirection) {
            calculateUserProgress(mRoutingPoints!!)
        }
    }

    private fun loadDirection(
        startPoint: LatLng,
        endPoint: LatLng,
        routingType: RoutingType,
        bearing: Int
    ) = callApi(
        navigationRepository.getDirection(routingType, startPoint, endPoint, bearing)
    ) { response ->
        if (response.routes != null) {

            mRoutingPoints = ArrayList()

            try {
                response.routes.firstOrNull()?.legs?.firstOrNull()?.let { leg ->

                    _routingDetail.postValue(leg)

                    leg.steps.map { step ->
                        mRoutingPoints!!.addAll(PolylineEncoding.decode(step.encodedPolyline))
                    }

                    if (mRoutingPoints!!.size >= 2) {
                        _progressPoints.postValue(mRoutingPoints!!)
                        _markerPosition.postValue(mRoutingPoints!!.first())
                    }
                }
            } catch (e: NullPointerException) {
                _generalError.postValue(Event(e.getError()))
                e.printStackTrace()
            }
        }
    }

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

    private companion object {
        const val MAX_DISTANCE_TOLERANCE_IN_METERS = 50
        const val MAX_BACKWARD_MOVEMENT_TOLERANCE_IN_METERS = 2
        const val DEFAULT_AVERAGE_SPEED_FOR_CAR = 200f
    }

}