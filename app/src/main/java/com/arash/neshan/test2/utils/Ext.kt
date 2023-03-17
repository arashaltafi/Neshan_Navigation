package com.arash.neshan.test2.utils

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.net.Uri
import android.os.Build
import android.view.View

fun Location.hasMock(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        this.isMock
    else
        this.isFromMockProvider

fun distanceInMeter(
    startLat: Double,
    startLon: Double,
    endLat: Double,
    endLon: Double
): Float {
    val startPoint = Location("locationA")
    startPoint.latitude = startLat
    startPoint.longitude = startLon

    val endPoint = Location("locationB")
    endPoint.latitude = endLat
    endPoint.longitude = endLon
    return startPoint.distanceTo(endPoint)
}

fun Activity.isDarkTheme(): Boolean {
    return this.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

fun View.toShow() {
    this.visibility = View.VISIBLE
}

fun View.isShow(): Boolean {
    return this.visibility == View.VISIBLE
}

fun View.toHide() {
    this.visibility = View.INVISIBLE
}

fun View.isHide(): Boolean {
    return this.visibility == View.INVISIBLE
}

fun View.toGone() {
    this.visibility = View.GONE
}

fun View.isGone(): Boolean {
    return this.visibility == View.GONE
}


fun Activity.openGoogleMapNavigation(markerLatitude: Double, markerLongitude: Double) =
    startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$markerLatitude,$markerLongitude&travelmode=driving")
        )
    )

fun Activity.openNeshanNavigation(markerLatitude: Double, markerLongitude: Double) =
    startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("nshn:$markerLatitude,$markerLongitude")
        )
    )

fun Activity.openChooseNavigation(markerLatitude: Double, markerLongitude: Double) =
    startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo: $markerLatitude,$markerLongitude")
        )
    )