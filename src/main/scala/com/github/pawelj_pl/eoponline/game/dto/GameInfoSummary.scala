package com.github.pawelj_pl.eoponline.game.dto

import com.github.pawelj_pl.eoponline.game.PlayerRole
import io.chrisdavenport.fuuid.circe._
import io.chrisdavenport.fuuid.FUUID
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

import java.time.Instant

final case class GameInfoSummary(
  id: FUUID,
  description: Option[String],
  playerNickname: String,
  ownerNickname: String,
  isOwner: Boolean,
  currentUserRole: Option[PlayerRole],
  startedAt: Option[Instant],
  finishedAt: Option[Instant])

object GameInfoSummary {

  implicit val encoder: Encoder[GameInfoSummary] = deriveEncoder

}
