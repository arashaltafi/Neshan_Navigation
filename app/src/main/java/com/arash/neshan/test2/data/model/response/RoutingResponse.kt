package com.arash.neshan.test2.data.model.response

import com.google.gson.annotations.SerializedName

data class RoutingResponse(
    val routes: ArrayList<Route>? = null,
) : NeshanResponse()

data class Route(
    @SerializedName("overview_polyline")
    val overviewPolyline: OverviewPolyline,
    val legs: ArrayList<Leg>
)

data class OverviewPolyline(
    @SerializedName("points")
    val encodedPolyline: String
)

data class Leg(
    val summary: String,
    val distance: Distance,
    val duration: Duration,
    val steps: ArrayList<Step>
)

data class Distance(val value: Int, val text: String)

data class Duration(val value: Int, val text: String)

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
)