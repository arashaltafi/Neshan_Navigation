package com.arash.neshan.test2.domain.model.error


class TimeoutError : GeneralError {
    companion object {
        fun instance() = TimeoutError()
    }
}
