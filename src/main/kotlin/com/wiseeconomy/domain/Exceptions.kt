package com.wiseeconomy.domain

sealed class AppError(msg: String?, cause: Throwable? = null): Throwable(msg, cause)

data class UserNotFound(val msg: String): AppError(msg)

data class InvalidAccessToken(val accessToken: String): AppError("Invalid access token: $accessToken")