package com.arash.neshan.test2.data.model

import android.os.Parcel
import android.os.Parcelable

/**
 * parcelable LatLng
 * */
data class LatLng(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readDouble(),
        parcel.readDouble()
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, p1: Int) {
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
    }

    companion object CREATOR : Parcelable.Creator<LatLng> {
        override fun createFromParcel(parcel: Parcel): LatLng {
            return LatLng(parcel)
        }

        override fun newArray(size: Int): Array<LatLng?> {
            return arrayOfNulls(size)
        }
    }
}