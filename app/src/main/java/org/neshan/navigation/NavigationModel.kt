package org.neshan.navigation

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.neshan.common.model.LatLng
import org.neshan.data.model.enums.RoutingType
import org.neshan.data.model.response.RoutingResponse
import org.neshan.data.network.ApiClient
import javax.inject.Inject

class NavigationModel @Inject constructor(private val mApiClient: ApiClient) {
    /**
     * loads routes from start point to end point from api service
     */
    fun getDirection(
        routType: RoutingType,
        start: LatLng,
        end: LatLng,
        bearing: Int
    ): Single<RoutingResponse> {
        val startPoint = start.latitude.toString() + "," + start.longitude
        val endPoint = end.latitude.toString() + "," + end.longitude
        return mApiClient.getDirection(routType.value, startPoint, endPoint, bearing)
            .subscribeOn(Schedulers.io())
    }
}
