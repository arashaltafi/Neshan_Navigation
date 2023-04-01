package com.arash.neshan.test2.domain.model.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class AddressDetailResponse(
    val neighbourhood: String? = "",
    val state: String? = "",
    val city: String? = "",
    @SerializedName("route_name")
    val routeName: String? = "",
    @SerializedName("formatted_address")
    val address: String? = ""
) : NeshanResponse(), Parcelable