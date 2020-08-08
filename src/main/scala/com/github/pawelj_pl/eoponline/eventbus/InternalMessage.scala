package com.github.pawelj_pl.eoponline.eventbus

import com.github.pawelj_pl.eoponline.game.{Game, PlayerRole}
import io.chrisdavenport.fuuid.circe._
import io.chrisdavenport.fuuid.FUUID
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

sealed trait InternalMessage

object InternalMessage {

  case object TopicStarted extends InternalMessage

  final case class GameCreated(game: Game) extends InternalMessage

  final case class ParticipantJoined(gameId: FUUID, userId: FUUID, nickName: String) extends InternalMessage

  final case class ParticipantKicked(gameId: FUUID, userId: FUUID) extends InternalMessage

  final case class RoleAssigned(gameId: FUUID, userId: FUUID, role: PlayerRole) extends InternalMessage

  object ParticipantJoined {

    implicit val encoder: Encoder[ParticipantJoined] = deriveEncoder

  }

  object ParticipantKicked {

    implicit val encoder: Encoder[ParticipantKicked] = deriveEncoder

  }

  object RoleAssigned {

    implicit val encoder: Encoder[RoleAssigned] = deriveEncoder

  }

}
