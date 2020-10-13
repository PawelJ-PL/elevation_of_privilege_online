package com.github.pawelj_pl.eoponline.`match`

import cats.instances.int._
import cats.syntax.eq._
import cats.syntax.show._
import com.github.pawelj_pl.eoponline.`match`.dto.{ExtendedDeckElementDto, GameStateDto}
import com.github.pawelj_pl.eoponline.`match`.repository.CardsRepository.CardsRepository
import com.github.pawelj_pl.eoponline.`match`.repository.{CardsRepository, GameplayRepository}
import com.github.pawelj_pl.eoponline.`match`.repository.GameplayRepository.GameplayRepository
import com.github.pawelj_pl.eoponline.game.PlayerRole
import com.github.pawelj_pl.eoponline.game.repository.GamesRepository
import com.github.pawelj_pl.eoponline.game.repository.GamesRepository.GamesRepository
import io.chrisdavenport.fuuid.FUUID
import io.github.gaelrenoux.tranzactio.doobie.Database
import io.scalaland.chimney.dsl._
import zio.{Has, ZIO, ZLayer}

object Matches {

  type Matches = Has[Matches.Service]

  trait Service {

    def getCurrentStateForPlayer(gameId: FUUID, playerId: FUUID): ZIO[Any, MatchInfoError, GameStateDto]

  }

  val live: ZLayer[Has[Database.Service] with GamesRepository with GameplayRepository with CardsRepository, Nothing, Matches] =
    ZLayer.fromServices[Database.Service, GamesRepository.Service, GameplayRepository.Service, CardsRepository.Service, Matches.Service] {
      (db, gamesRepo, gameplayRepo, cardsRepo) =>
        new Service {
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

        }
    }

}
