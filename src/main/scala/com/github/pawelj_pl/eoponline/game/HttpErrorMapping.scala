package com.github.pawelj_pl.eoponline.game

import org.http4s.{Response, Status}
import zio.{Task, UIO, ZIO}

object HttpErrorMapping {

  def mapGameError(error: GameError): UIO[Response[Task]] =
    error match {
      case error: GameInfoError => mapGameInfoError(error)
      case error: JoinGameError => mapJoinGameError(error)
      case error: KickUserError => mapKickError(error)
    }

  private def mapGameInfoError(error: GameInfoError): UIO[Response[Task]] =
    error match {
      case GameNotFound(_)               => ZIO.succeed(Response[Task](status = Status.NotFound))
      case ParticipantIsNotAMember(_, _) => ZIO.succeed(Response[Task](status = Status.NotFound))
      case ParticipantNotAccepted(_, _)  => ZIO.succeed(Response[Task](status = Status.Forbidden))
    }

  private def mapJoinGameError(error: JoinGameError): UIO[Response[Task]] =
    error match {
      case GameNotFound(_)                => ZIO.succeed(Response[Task](status = Status.NotFound))
      case ParticipantAlreadyJoined(_, _) => ZIO.succeed(Response[Task](status = Status.Conflict))
      case GameAlreadyStarted(_)          => ZIO.succeed(Response[Task](status = Status.NotFound))
    }

  private def mapKickError(error: KickUserError) =
    error match {
      case GameNotFound(_)          => ZIO.succeed(Response[Task](status = Status.NotFound))
      case UserIsNotGameOwner(_, _) => ZIO.succeed(Response[Task](status = Status.NotFound))
      case GameAlreadyStarted(_)    => ZIO.succeed(Response[Task](status = Status.NotFound))
      case KickSelfForbidden(_, _)  => ZIO.succeed(Response[Task](status = Status.PreconditionFailed))
    }

}
