package com.github.pawelj_pl.eoponline

import com.github.pawelj_pl.eoponline.database.Database
import com.github.pawelj_pl.eoponline.eventbus.handlers.WebSocketHandler
import zio.{ExitCode, Runtime, URIO, ZEnv, ZIO, App => ZioApp}
import zio.console.putStrLn

object Application extends ZioApp {

  private val app: ZIO[ZEnv, Throwable, Nothing] = (for {
    implicit0(runtime: Runtime[Environments.HttpServerEnvironment]) <- ZIO.runtime[Environments.HttpServerEnvironment].toManaged_
    _                                                               <- Database.migrate.toManaged_
    server = Http.server
    wsHandler = WebSocketHandler.handle.toManaged_
    _                                                               <- server <&> wsHandler
  } yield ()).useForever.provideSomeLayer(Environments.httpServerEnvironment)

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    app.foldCauseM(
      cause => putStrLn(cause.prettyPrint).as(ExitCode.failure),
      _ => ZIO.succeed(ExitCode.success)
    )

}
