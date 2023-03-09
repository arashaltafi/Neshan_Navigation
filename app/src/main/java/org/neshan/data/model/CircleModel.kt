package org.neshan.data.model

data class CircleModel(
    val name: String,
    val latLng: org.neshan.common.model.LatLng,
    val radiusMeter: Double,
)