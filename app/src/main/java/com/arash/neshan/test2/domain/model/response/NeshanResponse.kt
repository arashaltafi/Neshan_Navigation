package com.arash.neshan.test2.domain.model.response

abstract class NeshanResponse {
    val status: String = ""
    val code: Int? = null
    val message: String? = null

    fun isSuccessFull() = status.equals("OK", true)

}
