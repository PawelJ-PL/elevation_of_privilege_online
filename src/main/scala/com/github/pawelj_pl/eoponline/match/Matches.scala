package com.github.pawelj_pl.eoponline.`match`

import cats.instances.boolean._
import cats.instances.int._
import cats.syntax.eq._
import cats.syntax.show._
import com.github.pawelj_pl.eoponline.`match`.dto.{ExtendedDeckElementDto, GameStateDto, TableCardReqDto}
import com.github.pawelj_pl.eoponline.`match`.repository.CardsRepository.CardsRepository
import com.github.pawelj_pl.eoponline.`match`.repository.{CardsRepository, GameplayRepository}
import com.github.pawelj_pl.eoponline.`match`.repository.GameplayRepository.GameplayRepository
import com.github.pawelj_pl.eoponline.eventbus.InternalMessage
import com.github.pawelj_pl.eoponline.game.{Player, PlayerRole}
import com.github.pawelj_pl.eoponline.game.repository.GamesRepository
import com.github.pawelj_pl.eoponline.game.repository.GamesRepository.GamesRepository
import com.github.pawelj_pl.eoponline.utils.RandomUtils
import com.github.pawelj_pl.eoponline.utils.RandomUtils.RandomUtils
import fs2.concurrent.Topic
import io.chrisdavenport.fuuid.FUUID
import io.github.gaelrenoux.tranzactio.doobie.{Connection, Database}
import io.scalaland.chimney.dsl._
import zio.clock.Clock
import zio.{Has, Task, ZIO, ZLayer}

object Matches {

  type Matches = Has[Matches.Service]

  trait Service {

    def isAccepted(gameId: FUUID, playerId: FUUID): ZIO[Any, Nothing, Boolean]

    def getCurrentStateForPlayer(gameId: FUUID, playerId: FUUID): ZIO[Any, MatchInfoError, GameStateDto]

    def updateCardOnTableAs(gameId: FUUID, playerId: FUUID, cardNumber: Int, dto: TableCardReqDto): ZIO[Any, UpdateTableCardError, Unit]

    def putCardOnTheTableAs(gameId: FUUID, playerId: FUUID, cardNumber: Int): ZIO[Any, PutCardOnTableError, Unit]

  }

  val live: ZLayer[Has[Database.Service] with GamesRepository with GameplayRepository with CardsRepository with Has[
    Topic[Task, InternalMessage]
  ] with RandomUtils with Clock, Nothing, Matches] =
    ZLayer.fromServices[Database.Service, GamesRepository.Service, GameplayRepository.Service, CardsRepository.Service, Topic[
      Task,
      InternalMessage
    ], RandomUtils.Service, Clock.Service, Matches.Service] { (db, gamesRepo, gameplayRepo, cardsRepo, topic, random, clock) =>
      new Service {
        override def isAccepted(gameId: FUUID, playerId: FUUID): ZIO[Any, Nothing, Boolean] =
          db.transactionOrDie(
            gamesRepo.getAcceptedParticipants(gameId).orDie.map(_.map(_.id).contains(playerId))
          )

        override def getCurrentStateForPlayer(gameId: FUUID, playerId: FUUID): ZIO[Any, MatchInfoError, GameStateDto] =
          db.transactionOrDie(for {
            state        <- gameplayRepo.getState(gameId).orDie.someOrFail(GameNotFound(gameId))
            members      <- gamesRepo.getAcceptedParticipants(gameId).orDie
            _            <- ZIO.cond(members.map(_.id).contains(playerId), (), NotGameMember(gameId, playerId))
            cards        <- gameplayRepo.getCardsOf(gameId).orDie
            handElements = cards
                             .filter(card => card.playerId === playerId && card.location === CardLocation.Hand)
            tableElements = members
                              .filter(_.role.exists(_ === PlayerRole.Player))
                              .flatMap(player => cards.find(elem => elem.location === CardLocation.Table && elem.playerId === player.id))
            cardsOnHand  <- ZIO.foreach(handElements)(elem => toExtendedElement(elem).map(_.card)).orDie
            cardsOnTable <- ZIO.foreach(tableElements)(toExtendedElement).orDie
          } yield GameStateDto(state, cardsOnHand, cardsOnTable))

        private def toExtendedElement(element: DeckElement): ZIO[Any, Throwable, ExtendedDeckElementDto] =
          cardsRepo
            .get(element.cardNumber)
            .someOrFail(new RuntimeException(show"Definition for card ${element.cardNumber} not found"))
            .map(card => element.into[ExtendedDeckElementDto].withFieldConst(_.card, card).transform)

        override def updateCardOnTableAs(
          gameId: FUUID,
          playerId: FUUID,
          cardNumber: Int,
          dto: TableCardReqDto
        ): ZIO[Any, UpdateTableCardError, Unit] =
          db.transactionOrDie(
            for {
              state           <- gameplayRepo.getState(gameId).orDie.someOrFail(GameNotFound(gameId))
              _               <- canLinkThreat(gameId, playerId, cardNumber, state)
              _               <- gameplayRepo.updateThreatStatus(gameId, cardNumber, dto.threatLinked).orDie
              deck            <- gameplayRepo.getCardsOf(gameId).orDie
              _               <- topic.publish1(InternalMessage.ThreatLinkedStatusChanged(gameId, cardNumber, dto.threatLinked)).orDie
              maybeNextPlayer <- findNextPlayer(gameId, playerId, deck)
              _               <- maybeNextPlayer match {
                                   case Some(value) => handleNextPlayer(value.id, state)
                                   case None        => handleNextTurn(state, deck)
                                 }
            } yield ()
          )
        private def canLinkThreat(
          gameId: FUUID,
          playerId: FUUID,
          cardNumber: Int,
          state: GameState
        ): ZIO[Connection, UpdateTableCardError, Unit] =
          for {
            participant <- gamesRepo.getParticipantInfo(gameId, playerId).orDie.someOrFail(NotGameMember(gameId, playerId))
            _           <- ZIO.cond(participant.role.exists(_ === PlayerRole.Player), (), NotPlayer(gameId, playerId))
            deckElement <- canUseCard(state, cardNumber, CardLocation.Table, playerId)
            _           <- deckElement.threatLinked match {
                             case Some(value) => ZIO.fail(ThreatStatusAlreadyAssigned(gameId, cardNumber, value))
                             case None        => ZIO.unit
                           }
          } yield ()

        private def canUseCard(
          gameState: GameState,
          cardNumber: Int,
          expectedLocation: CardLocation,
          playerId: FUUID
        ): ZIO[Connection, CardVerificationError, DeckElement] =
          for {
            deckElement <-
              gameplayRepo.getSingleCardOf(gameState.gameId, cardNumber).orDie.someOrFail(CardNotFound(gameState.gameId, cardNumber))
            _           <- ZIO.cond(gameState.currentPlayer === playerId, (), OtherPlayersTurn(gameState.gameId, playerId, gameState.currentPlayer))
            _           <- ZIO.cond(
                             deckElement.playerId === playerId,
                             (),
                             CardOwnedByAnotherUser(gameState.gameId, cardNumber, playerId, deckElement.playerId)
                           )
            _           <- ZIO.cond(
                             deckElement.location === expectedLocation,
                             (),
                             UnexpectedCardLocation(gameState.gameId, cardNumber, expectedLocation, deckElement.location)
                           )
          } yield deckElement

        private def findNextPlayer(
          gameId: FUUID,
          currentPlayer: FUUID,
          cards: List[DeckElement]
        ): ZIO[Connection, Nothing, Option[Player]] =
          for {
            allPlayers <- gamesRepo.getPlayers(gameId).orDie
            (before, after) = allPlayers.splitAt(allPlayers.indexWhere(_.id === currentPlayer) + 1)
            nextPlayer = after
                           .find(p => isCandidateForNextPlayer(p.id, cards))
                           .orElse(before.find(p => isCandidateForNextPlayer(p.id, cards)))
          } yield nextPlayer

        private def isCandidateForNextPlayer(playerId: FUUID, allCards: List[DeckElement]): Boolean = {
          val playersOnTable = allCards.filter(_.location === CardLocation.Table).map(_.playerId)
          val cardsOnPlayersHand = allCards.filter(c => c.playerId === playerId && c.location === CardLocation.Hand)
          !playersOnTable.contains(playerId) && cardsOnPlayersHand.nonEmpty
        }

        private def handleNextPlayer(nextPlayer: FUUID, currentState: GameState): ZIO[Connection, Nothing, Unit] =
          for {
            _ <- gameplayRepo.updateState(currentState.gameId, nextPlayer, currentState.leadingSuit).orDie
            _ <- topic.publish1(InternalMessage.NextPlayer(currentState.gameId, nextPlayer)).orDie
          } yield ()

        private def handleNextTurn(gameState: GameState, cards: List[DeckElement]): ZIO[Connection, Nothing, Unit] =
          for {
            winners    <- findTurnWinners(cards.filter(_.location === CardLocation.Table), gameState)
            maybeWinner = winners.headOption
            _          <- maybeWinner match {
                            case Some(playerId) => gamesRepo.increaseTricksTaken(gameState.gameId, playerId).orDie
                            case None           => ZIO.unit
                          }
            _          <- topic.publish1(InternalMessage.PlayerTakesTrick(gameState.gameId, maybeWinner)).orDie
            initPlayer <- findNewRoundInitPlayer(cards, winners)
            _          <- initPlayer match {
                            case Some(value) => prepareNextTurn(value, gameState, cards)
                            case None        => handleMatchFinished(gameState.gameId)
                          }
          } yield ()

        private def findTurnWinners(cardsOnTable: List[DeckElement], gameState: GameState): ZIO[Any, Nothing, List[FUUID]] =
          for {
            describedCards <- ZIO.foreach(cardsOnTable)(toExtendedElement).orDie
            leadingSuit    <- ZIO.succeed(gameState.leadingSuit).someOrFail(new RuntimeException("Leading suit not defined")).orDie
            trump = describedCards
                      .filter(elem => elem.card.suit === Suit.ElevationOfPrivilege && elem.threatLinked.exists(_ === true))
                      .sortBy(_.card.cardNumber)
                      .reverse
            leading = describedCards
                        .filter(elem => elem.card.suit === leadingSuit && elem.threatLinked.exists(_ === true))
                        .sortBy(_.card.cardNumber)
                        .reverse
          } yield (trump ++ leading).map(_.playerId)

        private def findNewRoundInitPlayer(deck: List[DeckElement], possibleWinners: List[FUUID]): ZIO[Any, Nothing, Option[FUUID]] = {
          val playersWithCards: List[FUUID] = deck.filter(_.location === CardLocation.Hand).map(_.playerId)
          val firstWinnerWithCards = possibleWinners.find(playerId => playersWithCards.contains(playerId))
          firstWinnerWithCards match {
            case Some(value) => ZIO.succeed(Some(value))
            case None        => random.shuffle(playersWithCards).map(_.headOption)
          }
        }

        private def prepareNextTurn(nextPlayer: FUUID, state: GameState, deck: List[DeckElement]): ZIO[Connection, Nothing, Unit] =
          for {
            _ <- gameplayRepo.updateState(state.gameId, nextPlayer, None).orDie
            cardsOnTable = deck.filter(_.location === CardLocation.Table).map(_.cardNumber)
            _ <- gameplayRepo.updateCardsLocation(state.gameId, cardsOnTable, CardLocation.Out).orDie
            _ <- topic.publish1(InternalMessage.NextRound(state.gameId, nextPlayer)).orDie
          } yield ()

        private def handleMatchFinished(gameId: FUUID): ZIO[Connection, Nothing, Unit] =
          for {
            _   <- gameplayRepo.removeState(gameId).orDie
            now <- clock.instant
            _   <- gamesRepo.setFinishTime(gameId, now).orDie
            _   <- topic.publish1(InternalMessage.GameFinished(gameId)).orDie
          } yield ()

        override def putCardOnTheTableAs(gameId: FUUID, playerId: FUUID, cardNumber: Int): ZIO[Any, PutCardOnTableError, Unit] =
          db.transactionOrDie(
            for {
              state       <- gameplayRepo.getState(gameId).orDie.someOrFail(GameNotFound(gameId))
              participant <- gamesRepo.getParticipantInfo(gameId, playerId).orDie.someOrFail(NotGameMember(gameId, playerId))
              _           <- ZIO.cond(participant.role.exists(_ === PlayerRole.Player), (), NotPlayer(gameId, playerId))
              deckElement <- canUseCard(state, cardNumber, CardLocation.Hand, playerId)
              _           <- canCardBePlayedInThisTurn(state, deckElement)
              _           <- gameplayRepo.updateCardLocation(gameId, cardNumber, CardLocation.Table).orDie
              _           <- topic.publish1(InternalMessage.CardPlayed(gameId, playerId, cardNumber)).orDie
            } yield ()
          )

        private def canCardBePlayedInThisTurn(gameState: GameState, card: DeckElement): ZIO[Connection, PutCardOnTableError, Unit] =
          for {
            describedCard <- cardsRepo.get(card.cardNumber).someOrFail(new RuntimeException(show"Card ${card.cardNumber} not found")).orDie
            _             <- gameState.leadingSuit match {
                               case Some(leadingSuit) =>
                                 validateAgainstLeadingSuit(describedCard, leadingSuit, gameState.gameId, gameState.currentPlayer)
                               case None              =>
                                 gameplayRepo.updateState(gameState.gameId, gameState.currentPlayer, Some(describedCard.suit)).orDie
                             }
          } yield ()

        private def validateAgainstLeadingSuit(
          card: Card,
          leadingSuit: Suit,
          gameId: FUUID,
          player: FUUID
        ): ZIO[Connection, PutCardOnTableError, Unit] =
          for {
            deck              <- gameplayRepo.getCardsOf(gameId).orDie
            _                 <- deck.find(elem => elem.playerId === player && elem.location === CardLocation.Table) match {
                                   case Some(playedCard) => ZIO.fail(PlayerAlreadyPlayedCard(gameId, player, playedCard.cardNumber, card.cardNumber))
                                   case None             => ZIO.unit
                                 }
            onHands = deck.filter(elem => elem.playerId === player && elem.location === CardLocation.Hand)
            leadingSuitOnHand <- ZIO.foreach(onHands)(toExtendedElement).map(_.filter(_.card.suit === leadingSuit)).orDie
            _                 <- ZIO.cond(card.suit === leadingSuit, (), SuitDoesNotMatch(gameId, card.suit, leadingSuit)) <>
                                   ZIO.cond(leadingSuitOnHand.isEmpty, (), SuitDoesNotMatch(gameId, card.suit, leadingSuit))
          } yield ()
      }
    }

}
