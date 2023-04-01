package com.arash.neshan.test2.utils.location

import android.location.Location

interface LocationListener {

    fun onLastLocation(location: Location)

    fun onLocationChange(location: Location)

}