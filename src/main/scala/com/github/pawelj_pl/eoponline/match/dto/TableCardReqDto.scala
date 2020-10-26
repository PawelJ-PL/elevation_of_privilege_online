package com.github.pawelj_pl.eoponline.`match`.dto

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

final case class TableCardReqDto(threatLinked: Boolean)

object TableCardReqDto {
  implicit val decoder: Decoder[TableCardReqDto] = deriveDecoder
}