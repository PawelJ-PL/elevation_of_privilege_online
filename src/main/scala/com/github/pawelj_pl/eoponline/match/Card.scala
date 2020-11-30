package com.github.pawelj_pl.eoponline.`match`

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class Card(cardNumber: Int, value: Value, suit: Suit, text: String, example: String, mitigation: String)

object Card {

  implicit val codec: Codec[Card] = deriveCodec

}
