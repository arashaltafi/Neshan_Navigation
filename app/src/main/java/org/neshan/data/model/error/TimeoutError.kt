package org.neshan.data.model.error


class TimeoutError : GeneralError {
    companion object {
        fun instance() = TimeoutError()
    }
}
