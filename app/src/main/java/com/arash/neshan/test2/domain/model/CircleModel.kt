package com.arash.neshan.test2.domain.model

data class CircleModel(
    val title: String,
    val description: String,
    val latLng: org.neshan.common.model.LatLng,
    val radiusMeter: Double,
)