package com.arash.neshan.test2.utils.location

import android.location.Location
import android.os.CountDownTimer
import com.arash.neshan.test2.domain.model.response.Leg
import org.neshan.common.model.LatLng

class FakeLocationManager(private val routingDetail: Leg) :
    LocationManager {

    companion object {
        private const val CONSTANT_SPEED = 50 // unit -> kilometer per hour (kmh)
        private const val STEP_DISTANCE = CONSTANT_SPEED * 1000 / 60 / 60 // meter per second
    }

    private var mLocationListener: LocationListener? = null

    private var mTimer: CountDownTimer? = null

    private var mLastVisitedStepIndex: Int = 0

    private var mTraveledDistance: Int = -1

    override fun setLocationListener(listener: LocationListener) {
        mLocationListener = listener
    }

    override fun startLocationUpdates() {

        mLastVisitedStepIndex = 0
        mTraveledDistance = -1


        val totalDuration = (routingDetail.distance.value / STEP_DISTANCE) + 1
        startTimer(totalDuration)

    }

    override fun stopLocationUpdates() {
        mTimer?.cancel()
    }

    private fun startTimer(time: Int) {

        mTimer?.cancel()

        mTimer = object : CountDownTimer((time * 1000).toLong(), 1000) {

            override fun onTick(millisUntilFinished: Long) {
                calculateLocation()
            }

            override fun onFinish() {

            }
        }

        mTimer!!.start()

    }

    private fun calculateLocation() {

        // check if reached last step
        if (mLastVisitedStepIndex >= routingDetail.steps.lastIndex) {
            stopLocationUpdates()
            return
        }

        val latLng: LatLng
        var currentStep = routingDetail.steps[mLastVisitedStepIndex]
        if (mLastVisitedStepIndex == 0 && mTraveledDistance == -1) {
            // staring point
            latLng = LatLng(currentStep.startLocation[1], currentStep.startLocation[0])
            mTraveledDistance = 0
        } else {
            // add step distance to traveled distance
            mTraveledDistance += STEP_DISTANCE

            // calculate how many steps have been passed
            while (currentStep.distance.value < mTraveledDistance) {
                mTraveledDistance -= currentStep.distance.value
                mLastVisitedStepIndex++

                currentStep = routingDetail.steps[mLastVisitedStepIndex]

                // check if reached last step
                if (mLastVisitedStepIndex == routingDetail.steps.lastIndex) {
                    break
                }
            }

            // calculate new location
            latLng = if (mLastVisitedStepIndex == routingDetail.steps.lastIndex) {
                // if reached last step then last step should be new location
                LatLng(currentStep.startLocation[1], currentStep.startLocation[0])
            } else {
                // calculate location between current step and next step
                val nextStep = routingDetail.steps[mLastVisitedStepIndex + 1]
                val x = currentStep.distance.value / mTraveledDistance.toDouble()
                val latitude = currentStep.startLocation[1] + (nextStep.startLocation[1] - currentStep.startLocation[1]) / x
                val longitude = currentStep.startLocation[0] + (nextStep.startLocation[0] - currentStep.startLocation[0]) / x

                LatLng(latitude, longitude)
            }

        }

        // update location
        updateLocation(latLng)

    }

    private fun updateLocation(latLng: LatLng) {
        val location = Location("").apply {
            latitude = latLng.latitude
            longitude = latLng.longitude
        }
        mLocationListener?.onLocationChange(location)
    }

}