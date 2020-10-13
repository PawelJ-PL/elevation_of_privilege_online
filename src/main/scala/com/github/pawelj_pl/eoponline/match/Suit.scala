package com.github.pawelj_pl.eoponline.`match`

import enumeratum._

sealed trait Suit extends EnumEntry

object Suit extends Enum[Suit] with CirceEnum[Suit] with CatsEnum[Suit] {

  val values = findValues

  case object Spoofing extends Suit

  case object Tampering extends Suit

  case object Repudiation extends Suit

  case object InformationDisclosure extends Suit

  case object DenialOfService extends Suit

  case object ElevationOfPrivilege extends Suit

}
