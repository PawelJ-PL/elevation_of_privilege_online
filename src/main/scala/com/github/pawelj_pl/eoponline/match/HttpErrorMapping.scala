package com.github.pawelj_pl.eoponline.`match`

import com.github.pawelj_pl.eoponline.ResponseData
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.{Response, Status}
import zio.{Task, UIO, ZIO}

private[`match`] object HttpErrorMapping {

  def mapMatchInfoErrors(error: MatchInfoError): UIO[Response[Task]] =
    error match {
      case GameNotFound(_)     => ZIO.succeed(Response[Task](status = Status.NotFound).withEntity(ResponseData("Match not found")))
      case NotGameMember(_, _) => ZIO.succeed(Response[Task](status = Status.NotFound).withEntity(ResponseData("Match not found")))
    }

  def mapUpdateTableErrors(error: UpdateTableCardError): UIO[Response[Task]] =
    error match {
      case _: GameNotFound                => ZIO.succeed(Response[Task](status = Status.NotFound).withEntity(ResponseData("Match not found")))
      case _: NotGameMember               => ZIO.succeed(Response[Task](status = Status.NotFound).withEntity(ResponseData("Match not found")))
      case _: NotPlayer                   =>
        ZIO.succeed(
          Response[Task](status = Status.Forbidden).withEntity(ResponseData("Only player can change table status", Some("NotAPlayer")))
        )
      case _: OtherPlayersTurn            =>
        ZIO.succeed(Response[Task](status = Status.Forbidden).withEntity(ResponseData("Not your turn", Some("OtherPlayersTurn"))))
      case _: CardNotFound                =>
        ZIO.succeed(Response[Task](status = Status.PreconditionFailed).withEntity(ResponseData("Card not found", Some("CardNotFound"))))
      case _: CardOwnedByAnotherUser      =>
        ZIO.succeed(
          Response[Task](status = Status.Forbidden).withEntity(ResponseData("You don't have such card", Some("OtherPlayersCard")))
        )
      case _: UnexpectedCardLocation      =>
        ZIO.succeed(
          Response[Task](status = Status.PreconditionFailed).withEntity(ResponseData("Card must be on the table", Some("CardNotOnTable")))
        )
      case _: ThreatStatusAlreadyAssigned =>
        ZIO.succeed(
          Response[Task](status = Status.PreconditionFailed)
            .withEntity(ResponseData("Threat status already set", Some("ThreatStatusAlreadyAssigned")))
        )
    }

  def mapPutCardError(error: PutCardOnTableError): UIO[Response[Task]] =
    error match {
      case OtherPlayersTurn(_, _, _)           =>
        ZIO.succeed(Response[Task](status = Status.Forbidden).withEntity(ResponseData("Not your turn", Some("OtherPlayersTurn"))))
      case CardNotFound(_, _)                  =>
        ZIO.succeed(Response[Task](status = Status.PreconditionFailed).withEntity(ResponseData("Card not found", Some("CarNotFound"))))
      case CardOwnedByAnotherUser(_, _, _, _)  =>
        ZIO.succeed(
          Response[Task](status = Status.Forbidden).withEntity(ResponseData("You don't have such card", Some("OtherPlayersCard")))
        )
      case UnexpectedCardLocation(_, _, _, _)  =>
        ZIO.succeed(
          Response[Task](status = Status.PreconditionFailed).withEntity(ResponseData("Card must be on the hand", Some("CardNotOnTheHand")))
        )
      case GameNotFound(_)                     => ZIO.succeed(Response[Task](status = Status.NotFound).withEntity(ResponseData("Match not found")))
      case SuitDoesNotMatch(_, _, _)           =>
        ZIO.succeed(
          Response[Task](status = Status.PreconditionFailed)
            .withEntity(ResponseData("Selected suit doesn't match to leading suit", Some("SuitNotMatch")))
        )
      case PlayerAlreadyPlayedCard(_, _, _, _) =>
        ZIO.succeed(
          Response[Task](status = Status.PreconditionFailed)
            .withEntity(ResponseData("Player already played card", Some("PlayerAlreadyPlayedCard")))
        )
      case NotGameMember(_, _)                 =>
        ZIO.succeed(Response[Task](status = Status.NotFound).withEntity(ResponseData("Match not found")))
      case NotPlayer(_, _)                     =>
        ZIO.succeed(
          Response[Task](status = Status.Forbidden).withEntity(ResponseData("Only player can change table status", Some("NotAPlayer")))
        )
    }

  def mapGetScoresError(error: GetScoresError): UIO[Response[Task]] =
    error match {
      case NotGameMember(_, _) => ZIO.succeed(Response[Task](status = Status.NotFound).withEntity(ResponseData("Match not found")))
    }

}
