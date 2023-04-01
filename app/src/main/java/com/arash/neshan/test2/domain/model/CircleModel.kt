package com.arash.neshan.test2.domain.model

data class CircleModel(
    val name: String,
    val latLng: org.neshan.common.model.LatLng,
    val radiusMeter: Double,
)