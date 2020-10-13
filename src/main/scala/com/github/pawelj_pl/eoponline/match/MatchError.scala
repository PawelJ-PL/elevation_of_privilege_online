package com.github.pawelj_pl.eoponline.`match`

import io.chrisdavenport.fuuid.FUUID

sealed trait MatchError extends Product with Serializable {

  def asLogMessage: String = this.toString

}

sealed trait MatchInfoError extends MatchError

case class GameNotFound(gameId: FUUID) extends MatchInfoError

case class NotGameMember(gameId: FUUID, userId: FUUID) extends MatchInfoError
