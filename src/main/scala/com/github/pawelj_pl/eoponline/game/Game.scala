package com.github.pawelj_pl.eoponline.game

import java.time.Instant

import io.chrisdavenport.fuuid.circe._
import io.chrisdavenport.fuuid.FUUID
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

final case class Game(id: FUUID, description: Option[String], creator: FUUID, startedAt: Option[Instant])

object Game {

  implicit val encoder: Encoder[Game] = deriveEncoder

}
