package org.neshan.data.model.error

class NetworkError : GeneralError {
    companion object {
        fun instance() = NetworkError()
    }
}