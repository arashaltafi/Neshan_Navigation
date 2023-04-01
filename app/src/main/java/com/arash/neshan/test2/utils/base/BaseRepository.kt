package com.arash.neshan.test2.utils.base

import com.arash.neshan.test2.utils.flowIO
import retrofit2.Response

abstract class BaseRepository {

    fun <T> callApi(networkCall: suspend () -> Response<T>) = flowIO {
        val response = networkCall()
        emit(response)
    }

    fun <T> callDatabase(databaseCall: suspend () -> T) = flowIO {
        val response = databaseCall()
        emit(response)
    }
}