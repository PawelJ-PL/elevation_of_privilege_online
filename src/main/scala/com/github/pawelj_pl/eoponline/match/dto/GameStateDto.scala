package com.github.pawelj_pl.eoponline.`match`.dto

import com.github.pawelj_pl.eoponline.`match`.{Card, GameState}
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

final case class GameStateDto(state: GameState, hand: List[Card], table: List[ExtendedDeckElementDto])

object GameStateDto {

  implicit val encoder: Encoder[GameStateDto] = deriveEncoder

}
