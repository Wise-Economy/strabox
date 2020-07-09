package com.wiseeconomy

import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.core.Status.Companion.PERMANENT_REDIRECT

object Swagger {
    fun ui(descriptionPath: String): RoutingHttpHandler = routes(
            "/" bind Method.GET to {
                Response(PERMANENT_REDIRECT).header("Location", "/docs/index.html?url=$descriptionPath")
            },
            "/docs" bind static(Classpath("META-INF/resources/webjars/swagger-ui/3.28.0"))

    )
}
