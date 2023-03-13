package com.arash.neshan.test2.data.model.error


class TimeoutError : GeneralError {
    companion object {
        fun instance() = TimeoutError()
    }
}
