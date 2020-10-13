package com.github.pawelj_pl.eoponline.game

import cats.Eq
import enumeratum._

sealed trait PlayerRole extends EnumEntry

object PlayerRole extends Enum[PlayerRole] with CirceEnum[PlayerRole] {

  val values = findValues

  case object Player extends PlayerRole

  case object Observer extends PlayerRole

  object PlayerRoleVar {

    def unapply(str: String): Option[PlayerRole] = PlayerRole.withNameOption(str)

  }

  implicit val eq: Eq[PlayerRole] = Eq.fromUniversalEquals

}
