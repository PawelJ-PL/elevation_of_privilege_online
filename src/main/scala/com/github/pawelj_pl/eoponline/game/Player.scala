package com.github.pawelj_pl.eoponline.game

import io.chrisdavenport.fuuid.circe._
import io.chrisdavenport.fuuid.FUUID
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

final case class Player(id: FUUID, nickname: String, role: Option[PlayerRole])

object Player {

  implicit val encoder: Encoder[Player] = deriveEncoder

}
