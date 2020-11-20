package com.github.pawelj_pl.eoponline.session

import java.time.Instant

import io.chrisdavenport.fuuid.FUUID
import io.chrisdavenport.fuuid.circe._
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class Session(userId: FUUID, createdAt: Instant)

object Session {

  implicit val encoder: Codec[Session] = deriveCodec

}
