package com.arash.neshan.test2.utils.location

interface LocationManager {

    fun setLocationListener(listener: LocationListener)

    fun startLocationUpdates()

    fun stopLocationUpdates()
}