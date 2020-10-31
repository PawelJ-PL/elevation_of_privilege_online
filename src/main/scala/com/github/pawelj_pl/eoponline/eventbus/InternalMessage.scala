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

  final case class ThreatLinkedStatusChanged(gameId: FUUID, cardNumber: Int, newStatus: Boolean) extends InternalMessage

  final case class NextPlayer(gameId: FUUID, newPlayer: FUUID) extends InternalMessage

  final case class NextRound(gameId: FUUID, player: FUUID) extends InternalMessage

  final case class GameFinished(gameId: FUUID) extends InternalMessage

  final case class CardPlayed(gameId: FUUID, player: FUUID, cardNumber: Int) extends InternalMessage

  final case class PlayerTakesTrick(gameId: FUUID, player: Option[FUUID]) extends InternalMessage

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

  object ThreatLinkedStatusChanged {

    implicit val codec: Codec[ThreatLinkedStatusChanged] = deriveCodec

  }

  object NextPlayer {

    implicit val codec: Codec[NextPlayer] = deriveCodec

  }

  object NextRound {

    implicit val codec: Codec[NextRound] = deriveCodec

  }

  object GameFinished {

    implicit val codec: Codec[GameFinished] = deriveCodec

  }

  object CardPlayed {

    implicit val codec: Codec[CardPlayed] = deriveCodec

  }

  object PlayerTakesTrick {
    implicit val codec: Codec[PlayerTakesTrick] = deriveCodec
  }

}
