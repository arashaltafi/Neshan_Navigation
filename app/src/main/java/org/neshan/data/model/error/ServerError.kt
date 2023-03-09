package org.neshan.data.model.error

class ServerError(val statusCode: Int, val errorList: List<String>? = null) : GeneralError
