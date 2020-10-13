package com.github.pawelj_pl.eoponline.`match`

import io.chrisdavenport.fuuid.circe._
import io.chrisdavenport.fuuid.FUUID
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

final case class GameState(gameId: FUUID, currentPlayer: FUUID, leadingSuit: Option[Suit])

object GameState {

  implicit val encoder: Encoder[GameState] = deriveEncoder

}
