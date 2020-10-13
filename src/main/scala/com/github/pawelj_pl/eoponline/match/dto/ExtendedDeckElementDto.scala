package com.github.pawelj_pl.eoponline.`match`.dto

import com.github.pawelj_pl.eoponline.`match`.{Card, CardLocation}
import io.chrisdavenport.fuuid.circe._
import io.chrisdavenport.fuuid.FUUID
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

final case class ExtendedDeckElementDto(
  gameId: FUUID,
  playerId: FUUID,
  card: Card,
  location: CardLocation,
  threatLinked: Option[Boolean])

object ExtendedDeckElementDto {

  implicit val encoder: Encoder[ExtendedDeckElementDto] = deriveEncoder

}
