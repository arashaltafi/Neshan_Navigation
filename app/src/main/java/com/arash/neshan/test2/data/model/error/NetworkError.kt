package com.arash.neshan.test2.data.model.error

class NetworkError : GeneralError {
    companion object {
        fun instance() = NetworkError()
    }
}