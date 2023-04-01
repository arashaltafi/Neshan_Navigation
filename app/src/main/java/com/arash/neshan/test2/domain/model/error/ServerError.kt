package com.arash.neshan.test2.domain.model.error

class ServerError(val statusCode: Int, val errorList: List<String>? = null) : GeneralError
