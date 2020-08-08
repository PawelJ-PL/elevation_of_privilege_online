package com.github.pawelj_pl.eoponline.game.dto

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

final case class NewGameDto(description: Option[String], ownerNickname: String)

object NewGameDto {
  implicit val decoder: Decoder[NewGameDto] = deriveDecoder
}