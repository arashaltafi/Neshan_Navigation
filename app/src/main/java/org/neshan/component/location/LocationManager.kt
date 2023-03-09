package org.neshan.component.location

interface LocationManager {

    fun setLocationListener(listener: LocationListener)

    fun startLocationUpdates()

    fun stopLocationUpdates()
}