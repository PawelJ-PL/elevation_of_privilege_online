package com.github.pawelj_pl.eoponline.`match`

import io.chrisdavenport.fuuid.FUUID

sealed trait MatchError extends Product with Serializable {

  def asLogMessage: String = this.toString

}

sealed trait MatchInfoError extends MatchError

sealed trait UpdateTableCardError extends MatchError

sealed trait PutCardOnTableError extends MatchError

sealed trait CardVerificationError extends UpdateTableCardError with PutCardOnTableError

sealed trait GetScoresError extends MatchError

final case class GameNotFound(gameId: FUUID) extends MatchInfoError with UpdateTableCardError with PutCardOnTableError

final case class NotGameMember(gameId: FUUID, userId: FUUID)
    extends MatchInfoError
    with UpdateTableCardError
    with PutCardOnTableError
    with GetScoresError

final case class NotPlayer(gameId: FUUID, userId: FUUID) extends UpdateTableCardError with PutCardOnTableError

final case class OtherPlayersTurn(gameId: FUUID, requestedPlayer: FUUID, currentPlayer: FUUID) extends CardVerificationError

final case class CardNotFound(gameId: FUUID, cardNumber: Int) extends CardVerificationError

final case class CardOwnedByAnotherUser(gameId: FUUID, cardNumber: Int, requestedUser: FUUID, owner: FUUID) extends CardVerificationError

final case class UnexpectedCardLocation(gameId: FUUID, cardNumber: Int, expectedLocation: CardLocation, realLocation: CardLocation)
    extends CardVerificationError

final case class ThreatStatusAlreadyAssigned(gameId: FUUID, cardNumber: Int, threatLinked: Boolean) extends UpdateTableCardError

final case class SuitDoesNotMatch(gameId: FUUID, playedSuit: Suit, expectedSuit: Suit) extends PutCardOnTableError

final case class PlayerAlreadyPlayedCard(gameId: FUUID, playerId: FUUID, alreadyPlayed: Int, requested: Int) extends PutCardOnTableError
