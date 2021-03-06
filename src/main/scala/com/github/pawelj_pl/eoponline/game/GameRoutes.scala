package com.github.pawelj_pl.eoponline.game

import com.github.pawelj_pl.eoponline.game.Games.Games
import com.github.pawelj_pl.eoponline.game.dto.{JoinPlayerDto, NewGameDto}
import com.github.pawelj_pl.eoponline.session.{Authentication, Session}
import com.github.pawelj_pl.eoponline.session.Authentication.{Authentication, SessionRoutes}
import com.github.pawelj_pl.eoponline.game.HttpErrorMapping.{
  mapAssignRoleError,
  mapDeleteGameError,
  mapGameInfoError,
  mapGetParticipantsError,
  mapJoinGameError,
  mapKickError,
  mapStartGameError
}
import com.github.pawelj_pl.eoponline.game.PlayerRole.PlayerRoleVar
import io.chrisdavenport.fuuid.http4s.FUUIDVar
import org.http4s.{AuthedRoutes, HttpRoutes, Response, Status}
import org.http4s.dsl.Http4sDsl
import zio.{Has, Task, UIO, ZIO, ZLayer}
import zio.interop.catz._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import zio.logging.{Logger, Logging}

object GameRoutes extends Http4sDsl[Task] {

  type GameRoutes = Has[GameRoutes.Service]

  trait Service {

    def routes: UIO[HttpRoutes[Task]]

  }

  val routes: ZIO[GameRoutes, Nothing, HttpRoutes[Task]] = ZIO.accessM[GameRoutes](_.get.routes)

  val live: ZLayer[Games with Authentication with Logging, Nothing, GameRoutes] =
    ZLayer.fromServices[Games.Service, Authentication.Service, Logger[String], GameRoutes.Service] { (games, auth, logger) =>
      new Service {
        override def routes: UIO[HttpRoutes[Task]] =
          auth
            .sessionMiddleware
            .map { sessionMiddleware =>
              val authedRoutes: SessionRoutes = AuthedRoutes.of[Session, Task] {
                case authReq @ POST -> Root as session                                                                                  =>
                  authReq.req.as[NewGameDto].flatMap { dto =>
                    games.create(dto, session.userId).map(Response[Task](status = Status.Ok).withEntity(_)).resurrect
                  }

                case GET -> Root as session                                                                                             =>
                  games.getAllGamesOfUser(session.userId).map(Response[Task](status = Status.Ok).withEntity(_)).resurrect
                case GET -> Root / FUUIDVar(gameId) as session                                                                          =>
                  games
                    .getInfoAs(gameId, session.userId)
                    .map(Response[Task](status = Status.Ok).withEntity(_))
                    .catchAll(error => logger.warn(error.asLogMessage) *> mapGameInfoError(error))
                    .resurrect
                case authReq @ PUT -> Root / FUUIDVar(gameId) as session                                                                =>
                  authReq.req.as[JoinPlayerDto].flatMap { dto =>
                    games
                      .joinGame(gameId, session.userId, dto.nickname)
                      .as(Response[Task](status = Status.NoContent))
                      .catchAll(error => logger.warn(error.asLogMessage) *> mapJoinGameError(error))
                      .resurrect
                  }
                case POST -> Root / FUUIDVar(gameId) as session                                                                         =>
                  games
                    .startGameAs(gameId, session.userId)
                    .as(Response[Task](status = Status.Ok))
                    .catchAll(error => logger.warn(error.asLogMessage) *> mapStartGameError(error))
                    .resurrect
                case GET -> Root / FUUIDVar(gameId) / "members" as session                                                              =>
                  games
                    .getParticipantsAs(gameId, session.userId)
                    .map(Response[Task](status = Status.Ok).withEntity(_))
                    .catchAll(error => logger.warn(error.asLogMessage) *> mapGetParticipantsError(error))
                    .resurrect

                case DELETE -> Root / FUUIDVar(gameId) / "members" / FUUIDVar(participantId) as session                                 =>
                  games
                    .kickUserAs(gameId, participantId, session.userId)
                    .as(Response[Task](status = Status.NoContent))
                    .catchAll(error => logger.warn(error.asLogMessage) *> mapKickError(error))
                    .resurrect

                case PUT -> Root / FUUIDVar(gameId) / "members" / FUUIDVar(participantId) / "roles" / PlayerRoleVar(newRole) as session =>
                  games
                    .assignRoleAs(gameId, participantId, newRole, session.userId)
                    .as(Response[Task](status = Status.NoContent))
                    .catchAll(error => logger.warn(error.asLogMessage) *> mapAssignRoleError(error))
                    .resurrect

                case DELETE -> Root / FUUIDVar(gameId) as session                                                                       =>
                  games
                    .deleteGameAs(gameId, session.userId)
                    .as(Response[Task](status = Status.NoContent))
                    .catchAll(error => logger.warn(error.asLogMessage) *> mapDeleteGameError(error))
                    .resurrect
              }
              sessionMiddleware(authedRoutes)
            }
      }
    }

}
