package com.github.pawelj_pl.eoponline.http.web

import cats.syntax.semigroupk._
import cats.effect.Blocker
import org.http4s.{HttpRoutes, StaticFile}
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.headers.Location
import org.http4s.server.staticcontent.{ResourceService, resourceService}
import zio.blocking.Blocking
import zio.{Has, Task, UIO, ZIO, ZLayer}
import zio.interop.catz._

object StaticRoutes {

  type StaticRoutes = Has[StaticRoutes.Service]

  trait Service {

    def routes: UIO[HttpRoutes[Task]]

  }

  val routes: ZIO[StaticRoutes, Nothing, HttpRoutes[Task]] = ZIO.accessM[StaticRoutes](_.get.routes)

  val live: ZLayer[Blocking, Nothing, StaticRoutes] = ZLayer.fromService[Blocking.Service, StaticRoutes.Service] { blocking =>
    new Service with Http4sDsl[Task] {

      private val catsBlocker: Blocker = Blocker.liftExecutionContext(blocking.blockingExecutor.asEC)

      override def routes: UIO[HttpRoutes[Task]] = {
        val staticFileConfig = ResourceService.Config[Task]("/static", catsBlocker, "/static")
        val staticFileRoutes: HttpRoutes[Task] = resourceService[Task](staticFileConfig)

        val indexResource = StaticFile.fromResource[Task]("/static/index.html", catsBlocker).getOrElseF(NotFound())

        val webRoutes: HttpRoutes[Task] = HttpRoutes.of[Task] {
          case GET -> Root         => PermanentRedirect(Location(uri"/web"))
          case GET -> Root / "web" => indexResource
          case GET -> "web" /: _   => indexResource
        }

        UIO.succeed(staticFileRoutes <+> webRoutes)
      }

    }
  }

}
