package com.github.pawelj_pl.eoponline.`match`

import com.github.pawelj_pl.eoponline.`match`.HttpErrorMapping.mapMatchError
import com.github.pawelj_pl.eoponline.`match`.Matches.Matches
import com.github.pawelj_pl.eoponline.session.Authentication.Authentication
import com.github.pawelj_pl.eoponline.session.{Authentication, Session}
import io.chrisdavenport.fuuid.http4s.FUUIDVar
import org.http4s.{AuthedRoutes, HttpRoutes, Response, Status}
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import zio.interop.catz._
import zio.logging.{Logger, Logging}
import zio.{Has, Task, UIO, ZIO, ZLayer}

object MatchRoutes {

  type MatchRoutes = Has[MatchRoutes.Service]

  trait Service {

    def routes: UIO[HttpRoutes[Task]]

  }

  val routes: ZIO[MatchRoutes, Nothing, HttpRoutes[Task]] = ZIO.accessM[MatchRoutes](_.get.routes)

  val live: ZLayer[Authentication with Matches with Logging, Nothing, MatchRoutes] =
    ZLayer.fromServices[Authentication.Service, Matches.Service, Logger[String], MatchRoutes.Service] { (authMiddleware, matches, logger) =>
      new Service with Http4sDsl[Task] {
        override def routes: UIO[HttpRoutes[Task]] =
          authMiddleware.middleware.map { middleware =>
            val authedRoutes = AuthedRoutes.of[Session, Task] {
              case GET -> Root / FUUIDVar(gameId) as session =>
                matches
                  .getCurrentStateForPlayer(gameId, session.userId)
                  .map(Response[Task](status = Status.Ok).withEntity(_))
                  .catchAll(error => logger.warn(error.asLogMessage) *> mapMatchError(error))
                  .resurrect
            }
            middleware(authedRoutes)
          }
      }
    }

}
