package com.arash.neshan.test2.domain.model.error

class NetworkError : GeneralError {
    companion object {
        fun instance() = NetworkError()
    }
}