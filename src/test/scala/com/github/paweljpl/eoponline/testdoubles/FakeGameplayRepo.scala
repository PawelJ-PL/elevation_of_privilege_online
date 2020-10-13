package com.github.paweljpl.eoponline.testdoubles

import cats.syntax.eq._
import cats.instances.int._
import com.github.pawelj_pl.eoponline.`match`
import com.github.pawelj_pl.eoponline.`match`.{GameState => DomainGameState}
import com.github.pawelj_pl.eoponline.game.Player
import com.github.pawelj_pl.eoponline.`match`.{CardLocation, DeckElement, Suit}
import com.github.pawelj_pl.eoponline.`match`.repository.GameplayRepository
import com.github.pawelj_pl.eoponline.`match`.repository.GameplayRepository.GameplayRepository
import doobie.util.transactor
import io.chrisdavenport.fuuid.FUUID
import io.github.gaelrenoux.tranzactio.DbException
import io.scalaland.chimney.dsl._
import zio.{Has, Ref, Task, ULayer, ZIO, ZLayer}

object FakeGameplayRepo {

  final case class DeckEntry(gameId: FUUID, cardNumber: Int, playerId: FUUID, location: CardLocation, threatLinked: Option[Boolean])

  final case class GameState(gameId: FUUID, currentPlayer: FUUID, leadingSuit: Option[Suit])

  final case class GameplayRepoState(decks: Set[DeckEntry] = Set.empty, gamesState: Set[GameState] = Set.empty)

  def withState(ref: Ref[GameplayRepoState]): ULayer[GameplayRepository] =
    ZLayer.succeed(new GameplayRepository.Service {

      override def initDeck(gameId: FUUID, hands: List[(Player, List[Int])]): ZIO[Has[transactor.Transactor[Task]], DbException, Unit] = {
        val entries = hands.flatMap {
          case (player, cards) => cards.map(cardNumber => DeckEntry(gameId, cardNumber, player.id, CardLocation.Hand, None))
        }.toSet
        ref.update(state => state.copy(decks = entries))
      }

      override def updateCardLocation(
        gameId: FUUID,
        cardNumber: Int,
        location: CardLocation
      ): ZIO[Has[transactor.Transactor[Task]], DbException, Unit] =
        ref.update { state =>
          val oldValue = state.decks.find(deck => deck.gameId === gameId && deck.cardNumber === cardNumber)
          val updatedState =
            oldValue
              .map(deck => (state.decks - deck) + DeckEntry(gameId, cardNumber, deck.playerId, location, deck.threatLinked))
              .getOrElse(state.decks)
          state.copy(decks = updatedState)
        }

      override def updateState(
        gameId: FUUID,
        currentPlayer: FUUID,
        leadingSuit: Option[Suit]
      ): ZIO[Has[transactor.Transactor[Task]], DbException, Unit] =
        ref.update { state =>
          val removedOld: Set[GameState] = state.gamesState.filter(_.gameId =!= gameId)
          val updated = removedOld + GameState(gameId, currentPlayer, leadingSuit)
          state.copy(gamesState = updated)
        }

      override def getCardsOf(gameId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, List[DeckElement]] =
        ref.get.map(_.decks.filter(_.gameId === gameId).toList.transformInto[List[DeckElement]])

      override def getState(gameId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, Option[`match`.GameState]] =
        ref.get.map(x => x.gamesState.find(_.gameId === gameId).map(_.transformInto[DomainGameState]))

    })

}
