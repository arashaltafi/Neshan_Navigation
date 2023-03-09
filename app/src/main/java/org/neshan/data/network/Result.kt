package org.neshan.data.network

/**
 * A generic class that holds a value with its loading status.
 *
 * Result is usually created by the Repository classes where they return
 * `LiveData<Result<T>>` to pass back the latest data to the UI with its fetch status.
 */
data class Result<out T>(
    val status: Status, val data: T?, val error: Throwable?
) {

    enum class Status {
        SUCCESS, ERROR, LOADING
    }

    companion object {
        fun <T> success(data: T): Result<T> {
            return Result(Status.SUCCESS, data, null)
        }

        fun <T> success(): Result<T> {
            return Result(Status.SUCCESS, null, null)
        }

        fun <T> error(error: Throwable? = null): Result<T> {
            return Result(Status.ERROR, null, error)
        }

        fun <T> loading(): Result<T> {
            return Result(Status.LOADING, null, null)
        }
    }

}