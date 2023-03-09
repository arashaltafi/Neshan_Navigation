package org.neshan.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.SingleObserver
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import org.neshan.R
import org.neshan.common.model.LatLng
import org.neshan.common.utils.PolylineEncoding
import org.neshan.component.util.getError
import org.neshan.data.model.enums.RoutingType
import org.neshan.data.model.error.GeneralError
import org.neshan.data.model.error.SimpleError
import org.neshan.data.model.response.AddressDetailResponse
import org.neshan.data.model.response.RoutingResponse
import org.neshan.data.model.response.Step
import org.neshan.data.network.Result
import org.neshan.data.network.Result.Companion.error
import org.neshan.data.network.Result.Companion.loading
import org.neshan.data.network.Result.Companion.success
import org.neshan.data.util.Event
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(application: Application, private val mModel: MainModel) :
    AndroidViewModel(application) {
    private val mCompositeDisposable: CompositeDisposable = CompositeDisposable()

    // used for posting possible errors to view
    private val mGeneralError: MutableLiveData<Event<GeneralError>> = MutableLiveData()

    // address detail for selected location
    private val mLocationAddressDetail: MutableLiveData<Result<AddressDetailResponse>> =
        MutableLiveData()

    // calculated path for selected start and end points
    private val mRoutingDetail: MutableLiveData<RoutingResponse> = MutableLiveData()

    // points for showing direction path on map
    private val mRoutePoints: MutableLiveData<ArrayList<LatLng>> = MutableLiveData()

    // navigation start point
    var startPoint: LatLng? = null

    // navigation end point
    var endPoint: LatLng? = null

    val generalErrorLiveData: LiveData<Event<GeneralError>>
        get() = mGeneralError
    val locationAddressDetailLiveData: LiveData<Result<AddressDetailResponse>>
        get() = mLocationAddressDetail
    val routingDetailLiveData: LiveData<RoutingResponse>
        get() = mRoutingDetail
    val routePoints: LiveData<ArrayList<LatLng>>
        get() = mRoutePoints

    /**
     * try to load address detail from server
     */
    fun loadAddressForLocation(latLng: LatLng) {
        mLocationAddressDetail.postValue(loading())
        mModel.getAddress(latLng.latitude, latLng.longitude)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<AddressDetailResponse> {
                override fun onSubscribe(disposable: Disposable) {
                    mCompositeDisposable.add(disposable)
                }

                override fun onSuccess(response: AddressDetailResponse) {
                    if (response.isSuccessFull()) {
                        mLocationAddressDetail.postValue(success(response))
                    }
                }

                override fun onError(e: Throwable) {
                    mLocationAddressDetail.postValue(error(e))
                    mGeneralError.postValue(Event(e.getError()))
                }
            })
    }

    /**
     * try to load direction detail from server
     */
    fun loadDirection(routingType: RoutingType?) {
        if (startPoint == null) {
            val error =
                SimpleError(getApplication<Application>().getString(R.string.start_point_not_selected))
            mGeneralError.postValue(Event<GeneralError>(error))
        } else if (endPoint == null) {
            val error =
                SimpleError(getApplication<Application>().getString(R.string.end_point_not_selected))
            mGeneralError.postValue(Event<GeneralError>(error))
        } else {
            mModel.getDirection(routingType!!, startPoint!!, endPoint!!, 0)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<RoutingResponse> {
                    override fun onSubscribe(disposable: Disposable) {
                        mCompositeDisposable.add(disposable)
                    }

                    override fun onSuccess(response: RoutingResponse) {
                        if (response.routes != null) {
                            mRoutingDetail.postValue(response)
                            try {
                                val route = response.routes[0]
                                val decodedStepByStepPath = java.util.ArrayList<LatLng>()
                                for ( step: Step in route.legs[0].steps) {
                                    decodedStepByStepPath.addAll(PolylineEncoding.decode(step.encodedPolyline))
                                }
                                mRoutePoints.postValue(decodedStepByStepPath)
                            } catch (exception: NullPointerException) {
                                val error =
                                    SimpleError(getApplication<Application>().getString(R.string.routing_failure))
                                mGeneralError.postValue(Event<GeneralError>(error))
                                exception.printStackTrace()
                            }
                        }
                    }

                    override fun onError(e: Throwable) {
                        mGeneralError.postValue(Event(e.getError()))
                    }
                })
        }
    }

    override fun onCleared() {

        // disposes any incomplete request to avoid possible error also unnecessary network usage
        if (!mCompositeDisposable.isDisposed) {
            mCompositeDisposable.dispose()
        }
        super.onCleared()
    }
}
