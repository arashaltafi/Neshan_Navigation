package com.arash.neshan.test2.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LatLng(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Parcelable