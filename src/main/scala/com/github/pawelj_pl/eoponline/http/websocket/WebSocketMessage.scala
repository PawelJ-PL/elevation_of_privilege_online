package com.github.pawelj_pl.eoponline.http.websocket

import com.github.pawelj_pl.eoponline.`match`.dto.ExtendedDeckElementDto
import com.github.pawelj_pl.eoponline.eventbus.InternalMessage.{
  ParticipantJoined,
  ParticipantKicked,
  RoleAssigned,
  ThreatLinkedStatusChanged,
  GameFinished => GameFinishedInternalMessage,
  GameStarted => GameStartedInternalMessage,
  NextPlayer => NextPlayerInternalMessage,
  NextRound => NextRoundInternalMessage,
  PlayerTakesTrick => PlayerTakesTrickInternalMessage,
  GameDeleted => GameDeletedMessage
}
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import io.circe.syntax._
import org.http4s.websocket.WebSocketFrame

sealed trait WebSocketMessage[A] {

  def recipient: MessageRecipient

  def payload: A

  def eventType: String

  def toFrame(implicit encoder: WsFrameEncoder[WebSocketMessage[A]]): WebSocketFrame = encoder.encode(this)

}

object WebSocketMessage {

  case object TopicStarted extends WebSocketMessage[String] {

    override def recipient: MessageRecipient = MessageRecipient.Broadcast

    override def payload: String = "WebSocket topic has started"

    override def eventType: String = "WebSocketTopicStarted"

  }

  final case class NewParticipant(recipient: MessageRecipient, payload: ParticipantJoined) extends WebSocketMessage[ParticipantJoined] {

    override def eventType: String = "NewParticipant"

  }

  final case class ParticipantRemoved(recipient: MessageRecipient, payload: ParticipantKicked) extends WebSocketMessage[ParticipantKicked] {

    override def eventType: String = "ParticipantRemoved"

  }

  final case class UserRoleChanged(recipient: MessageRecipient, payload: RoleAssigned) extends WebSocketMessage[RoleAssigned] {

    override def eventType: String = "UserRoleChanged"

  }

  final case class GameStarted(recipient: MessageRecipient, payload: GameStartedInternalMessage)
      extends WebSocketMessage[GameStartedInternalMessage] {

    override def eventType: String = "GameStarted"

  }

  final case class ThreatStatusAssigned(recipient: MessageRecipient, payload: ThreatLinkedStatusChanged)
      extends WebSocketMessage[ThreatLinkedStatusChanged] {

    override def eventType: String = "ThreatStatusAssigned"

  }

  final case class NextPlayer(recipient: MessageRecipient, payload: NextPlayerInternalMessage)
      extends WebSocketMessage[NextPlayerInternalMessage] {

    override def eventType: String = "NextPlayer"

  }

  final case class NextRound(recipient: MessageRecipient, payload: NextRoundInternalMessage)
      extends WebSocketMessage[NextRoundInternalMessage] {

    override def eventType: String = "NextRound"

  }

  final case class GameFinished(recipient: MessageRecipient, payload: GameFinishedInternalMessage)
      extends WebSocketMessage[GameFinishedInternalMessage] {

    override def eventType: String = "GameFinished"

  }

  final case class CardPlayed(recipient: MessageRecipient, payload: ExtendedDeckElementDto)
      extends WebSocketMessage[ExtendedDeckElementDto] {

    override def eventType: String = "CardPlayed"

  }

  final case class PlayerTakesTrick(recipient: MessageRecipient, payload: PlayerTakesTrickInternalMessage)
      extends WebSocketMessage[PlayerTakesTrickInternalMessage] {

    override def eventType: String = "PlayerTakesTrick"

  }

  final case class GameDeleted(recipient: MessageRecipient, payload: GameDeletedMessage) extends WebSocketMessage[GameDeletedMessage] {

    override def eventType: String = "GameDeleted"

  }

  implicit val encoder: Encoder[WebSocketMessage[_]] = Encoder.instance {
    case TopicStarted            => encodeMessage(TopicStarted)
    case m: NewParticipant       => encodeMessage(m)
    case m: ParticipantRemoved   => encodeMessage(m)
    case m: UserRoleChanged      => encodeMessage(m)
    case m: GameStarted          => encodeMessage(m)
    case m: ThreatStatusAssigned => encodeMessage(m)
    case m: NextPlayer           => encodeMessage(m)
    case m: NextRound            => encodeMessage(m)
    case m: GameFinished         => encodeMessage(m)
    case m: CardPlayed           => encodeMessage(m)
    case m: PlayerTakesTrick     => encodeMessage(m)
    case m: GameDeleted          => encodeMessage(m)
  }

  private def encodeMessage[A: Encoder](message: WebSocketMessage[A]): Json =
    Json.obj(
      ("recipient" -> message.recipient.asJson),
      ("payload" -> message.payload.asJson),
      ("eventType" -> Json.fromString(message.eventType))
    )

  implicit val decoder: Decoder[WebSocketMessage[_]] = Decoder.instance { cursor =>
    for {
      recipient <- cursor.downField("recipient").as[MessageRecipient]
      eventType <- cursor.downField("eventType").as[String]
      payload   <- cursor.downField("payload").as[Json]
      result    <- decodeMessage(recipient, eventType, payload)
    } yield result
  }

  private def decodeMessage(
    messageRecipient: MessageRecipient,
    eventType: String,
    payload: Json
  ): Either[DecodingFailure, WebSocketMessage[_]] =
    eventType match {
      case "WebSocketTopicStarted" => Right(TopicStarted)
      case "NewParticipant"        => Decoder[ParticipantJoined].decodeJson(payload).map(p => NewParticipant(messageRecipient, p))
      case "ParticipantRemoved"    => Decoder[ParticipantKicked].decodeJson(payload).map(p => ParticipantRemoved(messageRecipient, p))
      case "UserRoleChanged"       => Decoder[RoleAssigned].decodeJson(payload).map(p => UserRoleChanged(messageRecipient, p))
      case "GameStarted"           => Decoder[GameStartedInternalMessage].decodeJson(payload).map(p => GameStarted(messageRecipient, p))
      case "ThreatStatusAssigned"  =>
        Decoder[ThreatLinkedStatusChanged].decodeJson(payload).map(p => ThreatStatusAssigned(messageRecipient, p))
      case "NextPlayer"            => Decoder[NextPlayerInternalMessage].decodeJson(payload).map(p => NextPlayer(messageRecipient, p))
      case "NextRound"             => Decoder[NextRoundInternalMessage].decodeJson(payload).map(p => NextRound(messageRecipient, p))
      case "GameFinished"          => Decoder[GameFinishedInternalMessage].decodeJson(payload).map(p => GameFinished(messageRecipient, p))
      case "CardPlayed"            => Decoder[ExtendedDeckElementDto].decodeJson(payload).map(p => CardPlayed(messageRecipient, p))
      case "PlayerTakesTrick"      =>
        Decoder[PlayerTakesTrickInternalMessage].decodeJson(payload).map(p => PlayerTakesTrick(messageRecipient, p))
      case "GameDeleted"           => Decoder[GameDeletedMessage].decodeJson(payload).map(p => GameDeleted(messageRecipient, p))
      case event                   => Left(DecodingFailure(s"Unknown event $event", List.empty))
    }

}

trait WsFrameEncoder[A] {

  def encode(a: A): WebSocketFrame

}

object WsFrameEncoder {

  def apply[A](implicit ev: WsFrameEncoder[A]): WsFrameEncoder[A] = ev

  object instances {

    implicit def webSocketMessageCirceEncoder[B: Encoder]: WsFrameEncoder[WebSocketMessage[B]] =
      (a: WebSocketMessage[B]) => {
        val json = Json.obj(
          "eventType" -> Json.fromString(a.eventType),
          "payload" -> Encoder[B].apply(a.payload)
        )
        WebSocketFrame.Text(json.noSpaces)
      }

  }

}
