package com.arash.neshan.test2.utils.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arash.neshan.test2.utils.viewModelIO
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.Response

open class BaseViewModel : ViewModel() {

    private val compositeDisposable by lazy { CompositeDisposable() }

    fun Disposable.addToComposite() {
        compositeDisposable.add(this)
    }

    fun <T> observeFlow(
        flow: Flow<T>,
        observeFunction: (T) -> Unit,
    ) = viewModelIO {
        flow.collect {
            observeFunction(it)
        }
    }

    /**
     * instead of [LiveData.postValue] use it
     * (when post multiple items at once! only last item will post!!)
     */
    fun <T> MutableLiveData<T>.setSafeValue(t: T?) {
        viewModelScope.launch { value = t }
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
        compositeDisposable.clear()
    }


    fun <T> callApi(
        networkCall: Flow<Response<T>>,
        liveResult: MutableLiveData<T>? = null,
        onResponse: ((T) -> Unit)? = null
    ) {
        val dispatchRetry: (() -> Unit)?
        dispatchRetry = {
            viewModelScope.launch {
                networkCall.collect { response ->
                    if (response.isSuccessful) {
                        response.body()?.let {
                            liveResult?.value = it
                            onResponse?.invoke(it)
                        }
                    }
                }
            }
        }
        dispatchRetry.invoke()
    }
}