package com.github.pawelj_pl.eoponline.`match`

import enumeratum._

sealed trait CardLocation extends EnumEntry

object CardLocation extends Enum[CardLocation] with CatsEnum[CardLocation] with CirceEnum[CardLocation] {

  val values = findValues

  case object Hand extends CardLocation

  case object Table extends CardLocation

  case object Out extends CardLocation

}
