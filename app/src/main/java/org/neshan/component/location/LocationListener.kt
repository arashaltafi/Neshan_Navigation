package org.neshan.component.location

import android.location.Location

interface LocationListener {

    fun onLastLocation(location: Location)

    fun onLocationChange(location: Location)

}