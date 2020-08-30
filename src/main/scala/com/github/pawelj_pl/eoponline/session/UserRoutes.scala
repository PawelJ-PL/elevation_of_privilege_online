package com.github.pawelj_pl.eoponline.session

import com.github.pawelj_pl.eoponline.session.Authentication.Authentication
import org.http4s.{AuthedRoutes, HttpRoutes, Response, Status}
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import zio.{Has, Task, UIO, ZIO, ZLayer}
import zio.interop.catz._

object UserRoutes {

  type UserRoutes = Has[UserRoutes.Service]

  trait Service {

    def routes: UIO[HttpRoutes[Task]]

  }

  val routes: ZIO[UserRoutes, Nothing, HttpRoutes[Task]] = ZIO.accessM[UserRoutes](_.get.routes)

  val live: ZLayer[Authentication, Nothing, UserRoutes] = ZLayer.fromService[Authentication.Service, UserRoutes.Service](auth =>
    new Service with Http4sDsl[Task] {

      override def routes: UIO[HttpRoutes[Task]] =
        auth.middleware.map { authMiddleware =>
          val authedRoutes = AuthedRoutes.of[Session, Task] {
            case GET -> Root / "me" as session =>
              Task.succeed(Response[Task](status = Status.Ok).withEntity(session))
          }
          authMiddleware(authedRoutes)
        }

    }
  )

}
