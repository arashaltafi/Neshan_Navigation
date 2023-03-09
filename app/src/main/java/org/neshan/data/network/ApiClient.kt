package org.neshan.data.network

import io.reactivex.rxjava3.core.Single
import org.neshan.data.model.response.AddressDetailResponse
import org.neshan.data.model.response.RoutingResponse
import retrofit2.http.GET
import org.neshan.data.model.enums.RoutingType
import retrofit2.http.Query

/**
 * The API interface for all required server apis
 */
interface ApiClient {

    /**
     * loads address detail for specific location by latitude and longitude
     * @param lat: latitude for desired location
     * @param lng: longitude for desired location
     */
    @GET("v4/reverse")
    fun getAddress(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): Single<AddressDetailResponse>

    /**
     * gets routes from start point to end point
     * @param type: routing type, one of [RoutingType] values
     * @param startPoint: start point coordinates formatted as "latitude,longitude"
     * @param endPoint: end point coordinates formatted as "latitude,longitude"
     * @param bearing: a value between 0 and 360
     */
    @GET("v4/direction") //@GET("v4/direction/no-traffic")
    fun getDirection(
        @Query("type") type: String,
        @Query("origin") startPoint: String,
        @Query("destination") endPoint: String,
        @Query("bearing") bearing: Int
    ): Single<RoutingResponse>

}