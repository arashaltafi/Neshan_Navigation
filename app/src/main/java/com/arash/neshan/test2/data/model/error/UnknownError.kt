package com.arash.neshan.test2.data.model.error

class UnknownError : GeneralError {
    companion object {
        fun instance() = UnknownError()
    }
}