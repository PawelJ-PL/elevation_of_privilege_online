package com.github.pawelj_pl.eoponline.game

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder._
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
        ZIO.succeed(Response[Task](status = Status.PreconditionFailed).withEntity("Game already started", Some("GameAlreadyStarted")))
    }

  private def mapKickError(error: KickUserError): UIO[Response[Task]] =
    error match {
      case GameNotFound(_)          =>
        ZIO.succeed(Response[Task](status = Status.NotFound).withEntity(ResponseData("Game not found")))
      case UserIsNotGameOwner(_, _) =>
        ZIO.succeed(Response[Task](status = Status.Forbidden).withEntity(ResponseData("User is not a game owner")))
      case GameAlreadyStarted(_)    =>
        ZIO.succeed(Response[Task](status = Status.PreconditionFailed).withEntity("Game already started", Some("GameAlreadyStarted")))
      case KickSelfForbidden(_, _)  =>
        ZIO.succeed(
          Response[Task](status = Status.PreconditionFailed)
            .withEntity(ResponseData("Kick self is not allowed", Some("KickSelfNotAllowed")))
        )
    }

}

final case class ResponseData(message: String, reason: Option[String] = None)

object ResponseData {

  implicit val encoder: Encoder[ResponseData] = deriveEncoder

}
