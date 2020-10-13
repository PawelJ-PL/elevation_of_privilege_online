package com.github.pawelj_pl.eoponline.`match`

import enumeratum._

sealed trait Value extends EnumEntry

object Value extends Enum[Value] with CirceEnum[Value] with CatsEnum[Value] {

  val values = findValues

  case object One extends Value

  case object Two extends Value

  case object Three extends Value

  case object Four extends Value

  case object Five extends Value

  case object Six extends Value

  case object Seven extends Value

  case object Eight extends Value

  case object Nine extends Value

  case object Ten extends Value

  case object Jack extends Value

  case object Queen extends Value

  case object King extends Value

  case object Ace extends Value

}
