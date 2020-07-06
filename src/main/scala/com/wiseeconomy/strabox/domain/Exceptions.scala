package com.wiseeconomy.strabox.domain

case object MissingAccessTokenHeader
    extends Exception("X-Access-Token header must be provided")

case object ExpiredAccessToken extends Exception("Access token expired")

case object UserNotFound extends Exception("User not found")