package com.arash.neshan.test2.component.location

import android.location.Location

interface LocationListener {

    fun onLastLocation(location: Location)

    fun onLocationChange(location: Location)

}