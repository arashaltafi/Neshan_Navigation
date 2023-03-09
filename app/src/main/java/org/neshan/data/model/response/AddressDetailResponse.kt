package org.neshan.data.model.response

import com.google.gson.annotations.SerializedName

data class AddressDetailResponse(
    val neighbourhood: String? = "",
    val state: String? = "",
    val city: String? = "",
    @SerializedName("route_name")
    val routeName: String? = "",
    @SerializedName("formatted_address")
    val address: String? = ""
) : NeshanResponse()