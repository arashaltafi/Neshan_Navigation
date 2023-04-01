package com.arash.neshan.test2.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arash.neshan.test2.R
import com.arash.neshan.test2.domain.model.enums.RoutingType
import com.arash.neshan.test2.domain.model.error.GeneralError
import com.arash.neshan.test2.domain.model.error.SimpleError
import com.arash.neshan.test2.domain.model.response.AddressDetailResponse
import com.arash.neshan.test2.domain.model.response.RoutingResponse
import com.arash.neshan.test2.domain.model.response.Step
import dagger.hilt.android.lifecycle.HiltViewModel
import com.arash.neshan.test2.domain.repository.NeshanRepository
import com.arash.neshan.test2.utils.base.BaseViewModel
import org.neshan.common.model.LatLng
import org.neshan.common.utils.PolylineEncoding
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val neshanRepository: NeshanRepository
) : BaseViewModel() {

    private val _liveLocationAddressDetail = MutableLiveData<AddressDetailResponse>()
    val liveLocationAddressDetail: LiveData<AddressDetailResponse>
        get() = _liveLocationAddressDetail

    private val _liveRoutePoints = MutableLiveData<RoutingResponse>()
    val liveRoutePoints: LiveData<RoutingResponse>
        get() = _liveRoutePoints

    private val _liveGeneralError = MutableLiveData<GeneralError>()
    val liveGeneralError: LiveData<GeneralError>
        get() = _liveGeneralError

    // points for showing direction path on map
    private val _routePoints = MutableLiveData<ArrayList<LatLng>>()
    val routePoints: MutableLiveData<ArrayList<LatLng>>
        get() = _routePoints

    // navigation start point
    var startPoint: LatLng? = null

    // navigation end point
    var endPoint: LatLng? = null

    /**
     * try to load address detail from server
     */
    fun loadAddressForLocation(
        latLng: LatLng
    ) = callApi(
        neshanRepository.getAddress(latLng.latitude, latLng.longitude),
        _liveLocationAddressDetail
    ) {
        loadDirection(RoutingType.CAR)
    }

    /**
     * try to load direction detail from server
     */
    private fun loadDirection(
        routingType: RoutingType
    ) {
        if (startPoint == null) {
            val error =
                SimpleError(R.string.start_point_not_selected)
            _liveGeneralError.postValue(error)
        } else if (endPoint == null) {
            val error =
                SimpleError(R.string.end_point_not_selected)
            _liveGeneralError.postValue(error)
        } else {

            callApi(
                neshanRepository.getDirection(
                    routingType, startPoint!!, endPoint!!, 0
                ),
                _liveRoutePoints
            ) { response ->
                if (response.routes != null) {
                    try {
                        val route = response.routes[0]
                        val decodedStepByStepPath = java.util.ArrayList<LatLng>()
                        for (step: Step in route.legs[0].steps) {
                            decodedStepByStepPath.addAll(PolylineEncoding.decode(step.encodedPolyline))
                        }
                        _routePoints.postValue(decodedStepByStepPath)
                    } catch (e: NullPointerException) {
                        val error = SimpleError(R.string.routing_failure)
                        _liveGeneralError.postValue(error)
                    }
                }
            }
        }
    }
}