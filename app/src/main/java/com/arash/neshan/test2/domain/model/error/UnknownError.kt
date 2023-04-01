package com.arash.neshan.test2.domain.model.error

class UnknownError : GeneralError {
    companion object {
        fun instance() = UnknownError()
    }
}