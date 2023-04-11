package com.arash.neshan.test2.domain.repository

import com.arash.neshan.test2.domain.model.enums.RoutingType
import com.arash.neshan.test2.domain.service.NeshanService
import com.arash.neshan.test2.utils.base.BaseRepository
import org.neshan.common.model.LatLng
import javax.inject.Inject


class NeshanRepository @Inject constructor(
    private val service: NeshanService
) : BaseRepository() {

    /**
     * loads address detail for specific location from api service
     */
    fun getAddress(
        latitude: Double,
        longitude: Double
    ) = callApi {
        service.getAddress(latitude, longitude)
    }


    /**
     * loads routes from start point to end point from api service
     */
    fun getDirection(
        routType: RoutingType,
        start: LatLng,
        end: LatLng,
        bearing: Int
    ) = callApi {
        val startPoint = start.latitude.toString() + "," + start.longitude
        val endPoint = end.latitude.toString() + "," + end.longitude
        service.getDirection(routType.value, startPoint, endPoint, bearing)
    }

}