package com.github.pawelj_pl.eoponline.`match`.repository

import com.github.pawelj_pl.eoponline.database.BasePostgresRepository
import com.github.pawelj_pl.eoponline.game.Player
import com.github.pawelj_pl.eoponline.`match`.{CardLocation, DeckElement, GameState, Suit}
import doobie.util.transactor
import io.chrisdavenport.fuuid.FUUID
import io.github.gaelrenoux.tranzactio.DbException
import io.github.gaelrenoux.tranzactio.doobie.{Connection, tzio}
import io.scalaland.chimney.dsl._
import zio.{Has, Task, ZIO, ZLayer}

object GameplayRepository {

  type GameplayRepository = Has[GameplayRepository.Service]

  trait Service {

    def initDeck(gameId: FUUID, hands: List[(Player, List[Int])]): ZIO[Connection, DbException, Unit]

    def getCardsOf(gameId: FUUID): ZIO[Connection, DbException, List[DeckElement]]

    def updateCardLocation(gameId: FUUID, cardNumber: Int, location: CardLocation): ZIO[Connection, DbException, Unit]

    def updateState(gameId: FUUID, currentPlayer: FUUID, leadingSuit: Option[Suit]): ZIO[Connection, DbException, Unit]

    def getState(gameId: FUUID): ZIO[Connection, DbException, Option[GameState]]

  }

  val postgres: ZLayer[Any, Nothing, GameplayRepository] = ZLayer.succeed(
    new Service with BasePostgresRepository {

      import doobieContext._
      import dbImplicits._

      private implicit val encodeLocation: MappedEncoding[CardLocation, String] = MappedEncoding[CardLocation, String](_.entryName)

      private implicit val decodeLocation: MappedEncoding[String, CardLocation] =
        MappedEncoding[String, CardLocation](CardLocation.withName)

      private implicit val encodeSuit: MappedEncoding[Suit, String] = MappedEncoding[Suit, String](_.entryName)

      private implicit val decodeSuit: MappedEncoding[String, Suit] = MappedEncoding[String, Suit](Suit.withName)

      override def initDeck(gameId: FUUID, hands: List[(Player, List[Int])]): ZIO[Has[transactor.Transactor[Task]], DbException, Unit] = {
        val flatten: List[DeckEntity] = hands
          .flatMap {
            case (player, cards) => cards.map((player, _))
          }
          .map {
            case (player, cardNumber) => DeckEntity(gameId, player.id, cardNumber, CardLocation.Hand, None)
          }
        tzio {
          run(
            quote(
              liftQuery(flatten).foreach(card => decks.insert(card))
            )
          )
        }
      }.unit

      override def getCardsOf(gameId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, List[DeckElement]] =
        tzio {
          run(
            quote(
              decks.filter(_.gameId == lift(gameId))
            )
          ).map(_.transformInto[List[DeckElement]])
        }

      override def updateCardLocation(gameId: FUUID, cardNumber: Int, location: CardLocation): ZIO[Connection, DbException, Unit] =
        tzio {
          run(
            quote(
              decks.filter(deck => deck.gameId == lift(gameId) && deck.cardNumber == lift(cardNumber)).update(_.location -> lift(location))
            )
          )
        }.unit

      override def updateState(
        gameId: FUUID,
        currentPlayer: FUUID,
        leadingSuit: Option[Suit]
      ): ZIO[Has[transactor.Transactor[Task]], DbException, Unit] =
        tzio {
          run(
            quote(
              gamesState
                .insert(GameStateEntity(lift(gameId), lift(currentPlayer), lift(leadingSuit)))
                .onConflictUpdate(_.gameId)(
                  (t, e) => t.gameId -> e.gameId,
                  (t, e) => t.currentPlayer -> e.currentPlayer,
                  (t, e) => t.leadingSuit -> e.leadingSuit
                )
            )
          )
        }.unit

      override def getState(gameId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, Option[GameState]] =
        tzio {
          run(
            quote(
              gamesState.filter(_.gameId == lift(gameId))
            )
          ).map(_.headOption).map(_.transformInto[Option[GameState]])
        }

      private val decks = quote {
        querySchema[DeckEntity]("decks")
      }

      private val gamesState = quote {
        querySchema[GameStateEntity]("games_state")
      }

    }
  )

}

private final case class DeckEntity(gameId: FUUID, playerId: FUUID, cardNumber: Int, location: CardLocation, threatLinked: Option[Boolean])

private final case class GameStateEntity(gameId: FUUID, currentPlayer: FUUID, leadingSuit: Option[Suit])
