package com.arash.neshan.test2.domain.service

import com.arash.neshan.test2.domain.model.response.AddressDetailResponse
import com.arash.neshan.test2.domain.model.response.RoutingResponse
import com.arash.neshan.test2.utils.base.BaseService
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NeshanService : BaseService {

    /**
     * loads address detail for specific location by latitude and longitude
     * @param lat: latitude for desired location
     * @param lng: longitude for desired location
     */
    @GET("v4/reverse")
    suspend fun getAddress(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): Response<AddressDetailResponse>

    /**
     * gets routes from start point to end point
     * @param type: routing type, one of [RoutingType] values
     * @param startPoint: start point coordinates formatted as "latitude,longitude"
     * @param endPoint: end point coordinates formatted as "latitude,longitude"
     * @param bearing: a value between 0 and 360
     */
    @GET("v4/direction") //@GET("v4/direction/no-traffic")
    suspend fun getDirection(
        @Query("type") type: String,
        @Query("origin") startPoint: String,
        @Query("destination") endPoint: String,
        @Query("bearing") bearing: Int
    ): Response<RoutingResponse>

}