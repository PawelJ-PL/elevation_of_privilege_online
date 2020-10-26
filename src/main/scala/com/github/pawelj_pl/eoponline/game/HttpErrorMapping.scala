package com.github.pawelj_pl.eoponline.game

import com.github.pawelj_pl.eoponline.ResponseData
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder._
import zio.{Task, UIO, ZIO}

private[game] object HttpErrorMapping {

  def mapGameError(error: GameError): UIO[Response[Task]] =
    error match {
      case error: GameInfoError        => mapGameInfoError(error)
      case error: JoinGameError        => mapJoinGameError(error)
      case error: KickUserError        => mapKickError(error)
      case error: StartGameError       => mapStartGameError(error)
      case error: AssignRoleError      => mapAssignRoleError(error)
      case error: GetParticipantsError => mapGetParticipantsError(error)
    }

  private def mapGameInfoError(error: GameInfoError): UIO[Response[Task]] =
    error match {
      case GameNotFound(_)               =>
        ZIO.succeed(Response[Task](status = Status.NotFound).withEntity(ResponseData("Game not found")))
      case ParticipantIsNotAMember(_, _) =>
        ZIO.succeed(Response[Task](status = Status.Forbidden).withEntity(ResponseData("User is not a games member", Some("NotAMember"))))
      case ParticipantNotAccepted(_, _)  =>
        ZIO.succeed(Response[Task](status = Status.Forbidden).withEntity(ResponseData("User not accepted", Some("NotAccepted"))))
    }

  private def mapJoinGameError(error: JoinGameError): UIO[Response[Task]] =
    error match {
      case GameNotFound(_)                =>
        ZIO.succeed(Response[Task](status = Status.NotFound).withEntity(ResponseData("Game not found")))
      case ParticipantAlreadyJoined(_, _) =>
        ZIO.succeed(Response[Task](status = Status.Conflict))
      case GameAlreadyStarted(_)          =>
        ZIO.succeed(
          Response[Task](status = Status.PreconditionFailed).withEntity(ResponseData("Game already started", Some("GameAlreadyStarted")))
        )
      case GameAlreadyFinished(_)         =>
        ZIO.succeed(Response[Task](status = Status.NotFound).withEntity(ResponseData("Game not found")))
    }

  private def mapKickError(error: KickUserError): UIO[Response[Task]] =
    error match {
      case GameNotFound(_)          =>
        ZIO.succeed(Response[Task](status = Status.NotFound).withEntity(ResponseData("Game not found")))
      case UserIsNotGameOwner(_, _) =>
        ZIO.succeed(Response[Task](status = Status.Forbidden).withEntity(ResponseData("User is not a game owner")))
      case GameAlreadyStarted(_)    =>
        ZIO.succeed(
          Response[Task](status = Status.PreconditionFailed).withEntity(ResponseData("Game already started", Some("GameAlreadyStarted")))
        )
      case KickSelfForbidden(_, _)  =>
        ZIO.succeed(
          Response[Task](status = Status.PreconditionFailed)
            .withEntity(ResponseData("Kick self is not allowed", Some("KickSelfNotAllowed")))
        )
      case GameAlreadyFinished(_)   =>
        ZIO.succeed(
          Response[Task](status = Status.PreconditionFailed).withEntity(ResponseData("Game already finished", Some("GameAlreadyFinished")))
        )
    }

  private def mapAssignRoleError(error: AssignRoleError): UIO[Response[Task]] =
    error match {
      case GameNotFound(_)          => ZIO.succeed(Response[Task](status = Status.NotFound).withEntity(ResponseData("Game not found")))
      case GameAlreadyStarted(_)    =>
        ZIO.succeed(
          Response[Task](status = Status.PreconditionFailed).withEntity(ResponseData("Game already started", Some("GameAlreadyStarted")))
        )
      case UserIsNotGameOwner(_, _) =>
        ZIO.succeed(Response[Task](status = Status.Forbidden).withEntity(ResponseData("User is not a game owner")))
      case GameAlreadyFinished(_)   =>
        ZIO.succeed(
          Response[Task](status = Status.PreconditionFailed).withEntity(ResponseData("Game already finished", Some("GameAlreadyFinished")))
        )
    }

  private def mapGetParticipantsError(error: GetParticipantsError): UIO[Response[Task]] =
    error match {
      case ParticipantIsNotAMember(_, _) =>
        ZIO.succeed(Response[Task](status = Status.Forbidden).withEntity(ResponseData("User is not a games member", Some("NotAMember"))))
      case ParticipantNotAccepted(_, _)  =>
        ZIO.succeed(Response[Task](status = Status.Forbidden).withEntity(ResponseData("User not accepted", Some("NotAccepted"))))
    }

  private def mapStartGameError(error: StartGameError): UIO[Response[Task]] =
    error match {
      case GameNotFound(_)          =>
        ZIO.succeed(
          Response[Task](status = Status.PreconditionFailed).withEntity(ResponseData("Game already started", Some("GameAlreadyStarted")))
        )
      case GameAlreadyStarted(_)    =>
        ZIO.succeed(
          Response[Task](status = Status.PreconditionFailed).withEntity(ResponseData("Game already started", Some("GameAlreadyStarted")))
        )
      case UserIsNotGameOwner(_, _) =>
        ZIO.succeed(Response[Task](status = Status.Forbidden).withEntity(ResponseData("User is not a game owner")))
      case GameAlreadyFinished(_)   =>
        ZIO.succeed(
          Response[Task](status = Status.PreconditionFailed).withEntity(ResponseData("Game already finished", Some("GameAlreadyFinished")))
        )
      case NotEnoughPlayers(_, _)   =>
        ZIO.succeed(
          Response[Task](status = Status.PreconditionFailed).withEntity(ResponseData("Not enough players", Some("NotEnoughPlayers")))
        )
      case TooManyPlayers(_, _)     =>
        ZIO.succeed(Response[Task](status = Status.PreconditionFailed).withEntity(ResponseData("Too many players", Some("TooManyPlayers"))))
    }

}

