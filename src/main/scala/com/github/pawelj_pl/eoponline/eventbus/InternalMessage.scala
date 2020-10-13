package com.github.pawelj_pl.eoponline.eventbus

import com.github.pawelj_pl.eoponline.game.{Game, PlayerRole}
import io.chrisdavenport.fuuid.circe._
import io.chrisdavenport.fuuid.FUUID
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

sealed trait InternalMessage

object InternalMessage {

  case object TopicStarted extends InternalMessage

  final case class GameCreated(game: Game) extends InternalMessage

  final case class ParticipantJoined(gameId: FUUID, userId: FUUID, nickName: String) extends InternalMessage

  final case class ParticipantKicked(gameId: FUUID, userId: FUUID) extends InternalMessage

  final case class RoleAssigned(gameId: FUUID, userId: FUUID, role: PlayerRole) extends InternalMessage

  final case class GameStarted(gameId: FUUID) extends InternalMessage

  object ParticipantJoined {

    implicit val codec: Codec[ParticipantJoined] = deriveCodec

  }

  object ParticipantKicked {

    implicit val codec: Codec[ParticipantKicked] = deriveCodec

  }

  object RoleAssigned {

    implicit val codec: Codec[RoleAssigned] = deriveCodec

  }

  object GameStarted {

    implicit val codec: Codec[GameStarted] = deriveCodec

  }

}
