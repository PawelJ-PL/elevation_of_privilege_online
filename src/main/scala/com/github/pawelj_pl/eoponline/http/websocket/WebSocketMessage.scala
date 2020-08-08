package com.github.pawelj_pl.eoponline.http.websocket

import com.github.pawelj_pl.eoponline.eventbus.InternalMessage.{ParticipantJoined, ParticipantKicked, RoleAssigned}
import io.circe.{Encoder, Json}
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
