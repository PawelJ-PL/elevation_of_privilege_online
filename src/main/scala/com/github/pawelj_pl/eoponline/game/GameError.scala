package com.github.pawelj_pl.eoponline.game

import io.chrisdavenport.fuuid.FUUID

sealed trait GameError extends Product with Serializable {

  def asLogMessage: String = this.toString

}

sealed trait GameInfoError extends GameError

sealed trait JoinGameError extends GameError

sealed trait GetParticipantsError extends GameError

sealed trait KickUserError extends GameError

sealed trait AssignRoleError extends GameError

case class GameNotFound(gameId: FUUID) extends GameInfoError with JoinGameError with KickUserError with AssignRoleError

case class ParticipantIsNotAMember(playerId: FUUID, gameId: FUUID) extends GameInfoError with GetParticipantsError

case class ParticipantNotAccepted(gameId: FUUID, playerId: Player) extends GameInfoError with GetParticipantsError

case class ParticipantAlreadyJoined(gameId: FUUID, player: Player) extends JoinGameError

case class GameAlreadyStarted(gameId: FUUID) extends JoinGameError with KickUserError with AssignRoleError

case class UserIsNotGameOwner(gameId: FUUID, userId: FUUID) extends KickUserError with AssignRoleError

case class KickSelfForbidden(gameId: FUUID, userId: FUUID) extends KickUserError
