package com.github.pawelj_pl.eoponline.http.websocket

import io.chrisdavenport.fuuid.FUUID

sealed trait MessageRecipient

object MessageRecipient {

  final case class SingleGameParticipant(gameId: FUUID, userId: FUUID) extends MessageRecipient

  final case class MultipleGameParticipants(gameId: FUUID, userIds: List[FUUID]) extends MessageRecipient

  final case object Broadcast extends MessageRecipient

}
