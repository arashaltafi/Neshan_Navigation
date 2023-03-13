package com.arash.neshan.test2.data.model.error

class ServerError(val statusCode: Int, val errorList: List<String>? = null) : GeneralError
