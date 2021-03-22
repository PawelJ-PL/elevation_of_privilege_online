package com.github.pawelj_pl.eoponline.http.websocket

import io.chrisdavenport.fuuid.circe._
import io.chrisdavenport.fuuid.FUUID
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

sealed trait MessageRecipient

object MessageRecipient {

  final case class AllGameParticipants(gameId: FUUID) extends MessageRecipient

  final case class SingleGameParticipant(gameId: FUUID, userId: FUUID) extends MessageRecipient

  final case class MultipleGameParticipants(gameId: FUUID, userIds: List[FUUID]) extends MessageRecipient

  final case object Broadcast extends MessageRecipient

  implicit val codec: Codec[MessageRecipient] = deriveCodec

}
