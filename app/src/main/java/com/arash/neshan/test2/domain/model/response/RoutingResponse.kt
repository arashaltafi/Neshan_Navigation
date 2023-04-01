package com.arash.neshan.test2.domain.model.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class RoutingResponse(
    val routes: ArrayList<Route>? = null,
) : NeshanResponse(), Parcelable

@Parcelize
data class Route(
    @SerializedName("overview_polyline")
    val overviewPolyline: OverviewPolyline,
    val legs: ArrayList<Leg>
) : Parcelable

@Parcelize
data class OverviewPolyline(
    @SerializedName("points")
    val encodedPolyline: String
) : Parcelable

@Parcelize
data class Leg(
    val summary: String,
    val distance: Distance,
    val duration: Duration,
    val steps: ArrayList<Step>
) : Parcelable

@Parcelize
data class Distance(val value: Int, val text: String) : Parcelable

@Parcelize
data class Duration(val value: Int, val text: String) : Parcelable

@Parcelize
data class Step(
    val name: String,
    val instruction: String,
    val distance: Distance,
    val duration: Duration,
    @SerializedName("start_location")
    val startLocation: DoubleArray,
    @SerializedName("bearing_after")
    val bearingAfter: Int,
    @SerializedName("polyline")
    val encodedPolyline: String
) : Parcelable