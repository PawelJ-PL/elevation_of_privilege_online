package com.github.pawelj_pl.eoponline.`match`

import com.github.pawelj_pl.eoponline.`match`.HttpErrorMapping.{mapGetScoresError, mapMatchInfoErrors, mapPutCardError, mapUpdateTableErrors}
import com.github.pawelj_pl.eoponline.`match`.Matches.Matches
import com.github.pawelj_pl.eoponline.`match`.dto.TableCardReqDto
import com.github.pawelj_pl.eoponline.session.Authentication.Authentication
import com.github.pawelj_pl.eoponline.session.{Authentication, Session}
import io.chrisdavenport.fuuid.FUUID
import io.chrisdavenport.fuuid.circe._
import io.chrisdavenport.fuuid.http4s.FUUIDVar
import io.circe.KeyEncoder
import org.http4s.{AuthedRoutes, HttpRoutes, Response, Status}
import org.http4s.circe.CirceEntityDecoder._
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
        private implicit val fuuidKeyEncoder: KeyEncoder[FUUID] = KeyEncoder.instance[FUUID](_.show)

        override def routes: UIO[HttpRoutes[Task]] =
          authMiddleware.middleware.map { middleware =>
            val authedRoutes = AuthedRoutes.of[Session, Task] {
              case GET -> Root / FUUIDVar(gameId) as session                                                      =>
                matches
                  .getCurrentStateForPlayer(gameId, session.userId)
                  .map(Response[Task](status = Status.Ok).withEntity(_))
                  .catchAll(error => logger.warn(error.asLogMessage) *> mapMatchInfoErrors(error))
                  .resurrect
              case GET -> Root / FUUIDVar(gameId) / "scores" as session                                           =>
                matches
                  .getScoresAs(gameId, session.userId)
                  .map(Response[Task](status = Status.Ok).withEntity(_))
                  .catchAll(error => logger.warn(error.asLogMessage) *> mapGetScoresError(error))
                  .resurrect
              case authReq @ PATCH -> Root / FUUIDVar(gameId) / "table" / "cards" / IntVar(cardNumber) as session =>
                authReq
                  .req
                  .as[TableCardReqDto]
                  .flatMap(dto =>
                    matches
                      .updateCardOnTableAs(gameId, session.userId, cardNumber, dto)
                      .map(_ => Response[Task](status = Status.NoContent))
                      .catchAll(error => logger.warn(error.asLogMessage) *> mapUpdateTableErrors(error))
                      .resurrect
                  )
              case PUT -> Root / FUUIDVar(gameId) / "table" / "cards" / IntVar(cardNumber) as session             =>
                matches
                  .putCardOnTheTableAs(gameId, session.userId, cardNumber)
                  .map(_ => Response[Task](status = Status.NoContent))
                  .catchAll(error => logger.warn(error.asLogMessage) *> mapPutCardError(error))
                  .resurrect
            }
            middleware(authedRoutes)
          }
      }
    }

}
