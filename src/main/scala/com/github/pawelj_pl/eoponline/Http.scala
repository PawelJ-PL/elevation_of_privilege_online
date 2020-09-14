package com.github.pawelj_pl.eoponline

import com.github.pawelj_pl.eoponline.config.AppConfig
import com.github.pawelj_pl.eoponline.game.GameRoutes
import com.github.pawelj_pl.eoponline.game.GameRoutes.GameRoutes
import com.github.pawelj_pl.eoponline.http.websocket.WebSocketRoutes
import com.github.pawelj_pl.eoponline.http.websocket.WebSocketRoutes.WebSocketRoutes
import com.github.pawelj_pl.eoponline.session.UserRoutes
import com.github.pawelj_pl.eoponline.session.UserRoutes.UserRoutes
import org.http4s.implicits._
import org.http4s.server.{Router, Server}
import org.http4s.server.blaze.BlazeServerBuilder
import zio.config.ZConfig
import zio.interop.catz._
import zio.interop.catz.implicits.ioTimer
import zio.{Runtime, Task, ZManaged}
import zio.config.getConfig

object Http {

  def server(
    implicit runtime: Runtime[Environments.HttpServerEnvironment]
  ): ZManaged[GameRoutes with ZConfig[AppConfig.Server] with WebSocketRoutes with UserRoutes, Throwable, Server[Task]] =
    for {
      apiRoutes <- routes.map(r => Router("/api/v1" -> r)).toManaged_
      config    <- getConfig[AppConfig.Server].toManaged_
      srv       <- BlazeServerBuilder[Task](runtime.platform.executor.asEC)
                     .bindHttp(config.port, config.host)
                     .withHttpApp(apiRoutes.orNotFound)
                     .resource
                     .toManagedZIO
    } yield srv

  private val routes = for {
    games     <- GameRoutes.routes
    webSocket <- WebSocketRoutes.routes
    users     <- UserRoutes.routes
  } yield Router(
    "/games" -> games,
    "/ws" -> webSocket,
    "/users" -> users
  )

}
