package com.github.pawelj_pl.eoponline

import cats.arrow.FunctionK
import cats.syntax.semigroupk._
import com.github.pawelj_pl.eoponline.config.AppConfig
import com.github.pawelj_pl.eoponline.game.GameRoutes
import com.github.pawelj_pl.eoponline.game.GameRoutes.GameRoutes
import com.github.pawelj_pl.eoponline.`match`.MatchRoutes
import com.github.pawelj_pl.eoponline.`match`.MatchRoutes.MatchRoutes
import com.github.pawelj_pl.eoponline.http.web.StaticRoutes
import com.github.pawelj_pl.eoponline.http.web.StaticRoutes.StaticRoutes
import com.github.pawelj_pl.eoponline.http.websocket.WebSocketRoutes
import com.github.pawelj_pl.eoponline.http.websocket.WebSocketRoutes.WebSocketRoutes
import com.github.pawelj_pl.eoponline.session.UserRoutes
import com.github.pawelj_pl.eoponline.session.UserRoutes.UserRoutes
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.HttpApp
import org.http4s.implicits._
import org.http4s.server.{Router, Server}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.RequestLogger
import zio.interop.catz._
import zio.interop.catz.implicits.ioTimer
import zio.{Has, Runtime, Task, ZManaged}
import zio.config.getConfig

object Http {

  def server(
    implicit runtime: Runtime[Environments.HttpServerEnvironment]
  ): ZManaged[GameRoutes with Has[
    AppConfig.Server
  ] with WebSocketRoutes with UserRoutes with MatchRoutes with StaticRoutes, Throwable, Server[Task]] =
    for {
      webRoutes <- StaticRoutes.routes.toManaged_
      apiRoutes <- routes.map(r => Router("/api/v1" -> r)).toManaged_
      config    <- getConfig[AppConfig.Server].toManaged_
      baseApp = (webRoutes <+> apiRoutes).orNotFound
      app = enrichedApp(baseApp, config.logger)
      srv       <- BlazeServerBuilder[Task](runtime.platform.executor.asEC)
                     .bindHttp(config.port, config.host)
                     .withHttpApp(app)
                     .resource
                     .toManagedZIO
    } yield srv

  private val routes = for {
    games     <- GameRoutes.routes
    webSocket <- WebSocketRoutes.routes
    users     <- UserRoutes.routes
    gameplay  <- MatchRoutes.routes
  } yield Router(
    "/games" -> games,
    "/matches" -> gameplay,
    "/ws" -> webSocket,
    "/users" -> users
  )

  private def enrichedApp(baseApp: HttpApp[Task], httpLoggerConfig: AppConfig.HttpLogger): HttpApp[Task] =
    if (!httpLoggerConfig.enabled)
      baseApp
    else
      RequestLogger(logHeaders = httpLoggerConfig.logHeaders, logBody = httpLoggerConfig.logBody, FunctionK.id[Task])(baseApp)

}

final case class ResponseData(message: String, reason: Option[String] = None)

object ResponseData {

  implicit val encoder: Encoder[ResponseData] = deriveEncoder

}
