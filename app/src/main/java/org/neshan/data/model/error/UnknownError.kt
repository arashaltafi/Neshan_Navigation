package org.neshan.data.model.error

class UnknownError : GeneralError {
    companion object {
        fun instance() = UnknownError()
    }
}