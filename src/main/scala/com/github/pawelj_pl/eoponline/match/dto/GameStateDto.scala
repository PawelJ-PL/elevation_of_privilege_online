package com.github.pawelj_pl.eoponline.`match`.dto

import com.github.pawelj_pl.eoponline.`match`.{Card, GameState}
import io.chrisdavenport.fuuid.circe._
import io.chrisdavenport.fuuid.FUUID
import io.circe.{Encoder, KeyEncoder}
import io.circe.generic.semiauto.deriveEncoder

final case class GameStateDto(state: GameState, hand: List[Card], table: List[ExtendedDeckElementDto], playersScores: Map[FUUID, Int])

object GameStateDto {

  implicit val fuuidKeyEncoder: KeyEncoder[FUUID] = KeyEncoder.instance[FUUID](_.show)

  implicitly[Encoder[Map[Int, Int]]]

  implicit val encoder: Encoder[GameStateDto] = deriveEncoder

}
