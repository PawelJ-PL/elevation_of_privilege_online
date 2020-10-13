package com.github.pawelj_pl.eoponline.`match`

import io.chrisdavenport.fuuid.FUUID

final case class DeckElement(gameId: FUUID, playerId: FUUID, cardNumber: Int, location: CardLocation, threatLinked: Option[Boolean])
