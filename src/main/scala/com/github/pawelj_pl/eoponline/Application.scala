package com.github.pawelj_pl.eoponline

import com.github.pawelj_pl.eoponline.database.Database
import com.github.pawelj_pl.eoponline.eventbus.handlers.WebSocketHandler
import com.github.pawelj_pl.eoponline.scheduler.GameCleanup
import zio.{ExitCode, Runtime, URIO, ZEnv, ZIO, App => ZioApp}

object Application extends ZioApp {

  private val app: ZIO[ZEnv, Throwable, Nothing] =
    (for {
      implicit0(runtime: Runtime[Environments.HttpServerEnvironment]) <- ZIO.runtime[Environments.HttpServerEnvironment].toManaged_
      _                                                               <- Database.migrate.toManaged_
      server = Http.server
      wsHandler = WebSocketHandler.handle.toManaged_
      cleanupScheduler = GameCleanup.schedule.toManaged_
      _                                                               <- server <&> wsHandler <&> cleanupScheduler
    } yield ())
      .useForever
      .provideSomeLayer(Environments.httpServerEnvironment)

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = app.exitCode

}
