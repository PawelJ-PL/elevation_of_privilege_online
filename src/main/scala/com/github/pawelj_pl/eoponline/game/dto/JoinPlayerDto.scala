package com.github.pawelj_pl.eoponline.game.dto

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

final case class JoinPlayerDto(nickname: String)

object JoinPlayerDto {

  implicit val decoder: Decoder[JoinPlayerDto] = deriveDecoder

}
