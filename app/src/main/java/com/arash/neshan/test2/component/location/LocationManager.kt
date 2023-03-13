package com.arash.neshan.test2.component.location

interface LocationManager {

    fun setLocationListener(listener: LocationListener)

    fun startLocationUpdates()

    fun stopLocationUpdates()
}