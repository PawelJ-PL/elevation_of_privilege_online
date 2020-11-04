package com.github.pawelj_pl.eoponline.game.repository

import java.time.Instant

import cats.instances.long._
import cats.syntax.eq._
import com.github.pawelj_pl.eoponline.database.BasePostgresRepository
import com.github.pawelj_pl.eoponline.game.{Game, Player, PlayerRole}
import doobie.util.transactor
import io.chrisdavenport.fuuid.FUUID
import io.getquill.Ord
import io.github.gaelrenoux.tranzactio.doobie._
import io.scalaland.chimney.dsl._
import zio.{Has, Task, ZIO, ZLayer}
import io.github.gaelrenoux.tranzactio.DbException
import zio.clock.Clock

object GamesRepository {

  type GamesRepository = Has[GamesRepository.Service]

  trait Service {

    def findById(gameId: FUUID): ZIO[Connection, DbException, Option[Game]]

    def getActiveGame(gameId: FUUID): ZIO[Connection, DbException, Option[Game]]

    def create(game: Game): ZIO[Connection, DbException, Game]

    def setStartTime(gameId: FUUID, time: Instant): ZIO[Connection, DbException, Unit]

    def setFinishTime(gameId: FUUID, time: Instant): ZIO[Connection, DbException, Unit]

    def getGamesOlderThan(time: Instant): ZIO[Connection, DbException, List[Game]]

    def deleteGames(gameIds: List[FUUID]): ZIO[Connection, DbException, Long]

    def addPlayer(gameId: FUUID, player: Player): ZIO[Connection, DbException, Unit]

    def getParticipantInfo(gameId: FUUID, playerId: FUUID): ZIO[Connection, DbException, Option[Player]]

    def getParticipants(gameId: FUUID): ZIO[Connection, DbException, List[Player]]

    def getAcceptedParticipants(gameId: FUUID): ZIO[Connection, DbException, List[Player]]

    def getPlayers(gameId: FUUID): ZIO[Connection, DbException, List[Player]]

    def removePlayer(gameId: FUUID, playerId: FUUID): ZIO[Connection, DbException, Boolean]

    def removePlayers(gameId: FUUID, playerIds: List[FUUID]): ZIO[Connection, DbException, Boolean]

    def assignRole(gameId: FUUID, playerId: FUUID, role: PlayerRole): ZIO[Connection, DbException, Boolean]

    def increaseTricksTaken(gameId: FUUID, playerId: FUUID, increaseBy: Int = 1): ZIO[Connection, DbException, Unit]

    def getPlayersTricks(gameId: FUUID): ZIO[Connection, DbException, Map[FUUID, Int]]

  }

  val postgres: ZLayer[Clock, Nothing, GamesRepository] = ZLayer.fromService { clock =>
    new Service with BasePostgresRepository {

      import doobieContext._
      import dbImplicits._

      private implicit val encodeRole: MappedEncoding[PlayerRole, String] = MappedEncoding[PlayerRole, String](_.entryName)
      private implicit val decodeRole: MappedEncoding[String, PlayerRole] = MappedEncoding[String, PlayerRole](PlayerRole.withName)

      override def findById(gameId: FUUID): ZIO[Connection, DbException, Option[Game]] =
        tzio {
          run(
            quote(
              games.filter(_.id == lift(gameId))
            )
          ).map(_.headOption.map(_.transformInto[Game]))
        }

      override def getActiveGame(gameId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, Option[Game]] =
        tzio {
          run(
            quote(
              games.filter(game => game.id == lift(gameId) && game.startedAt.isDefined && game.finishedAt.isEmpty)
            )
          ).map(_.headOption.map(_.transformInto[Game]))
        }

      override def create(game: Game): ZIO[Connection, DbException, Game] =
        for {
          now <- clock.instant
          entity = game
                     .into[GameEntity]
                     .withFieldConst(_.updatedAt, now)
                     .withFieldConst(_.createdAt, now)
                     .transform
          _   <- tzio(run(quote(games.insert(lift(entity)))))
        } yield entity.transformInto[Game]

      override def setStartTime(gameId: FUUID, time: Instant): ZIO[Has[transactor.Transactor[Task]], DbException, Unit] =
        for {
          now <- clock.instant
          _   <- tzio {
                   run(
                     quote(
                       games
                         .filter(_.id == lift(gameId))
                         .update(game => game.startedAt -> lift(Option(time)), game => game.updatedAt -> lift(now))
                     )
                   )
                 }
        } yield ()

      override def setFinishTime(gameId: FUUID, time: Instant): ZIO[Has[transactor.Transactor[Task]], DbException, Unit] =
        for {
          now <- clock.instant
          _   <- tzio {
                   run(
                     quote(
                       games
                         .filter(_.id == lift(gameId))
                         .update(game => game.finishedAt -> lift(Option(time)), game => game.updatedAt -> lift(now))
                     )
                   )
                 }
        } yield ()

      override def getGamesOlderThan(time: Instant): ZIO[Has[transactor.Transactor[Task]], DbException, List[Game]] =
        tzio {
          run(
            quote(
              games.filter(_.updatedAt < lift(time))
            )
          ).map(_.transformInto[List[Game]])
        }

      override def deleteGames(gameIds: List[FUUID]): ZIO[Has[transactor.Transactor[Task]], DbException, Long] =
        tzio {
          run(
            quote(
              games.filter(g => liftQuery(gameIds).contains(g.id)).delete
            )
          )
        }

      override def addPlayer(gameId: FUUID, player: Player): ZIO[Connection, DbException, Unit] =
        tzio {
          for {
            maybeMaxOrder <- run(quote(gamePlayers.filter(_.gameId == lift(gameId)).map(_.playerOrder).max))
            maxOrder = maybeMaxOrder.getOrElse(0)
            _             <- run(
                               quote(
                                 gamePlayers
                                   .insert(
                                     lift(
                                       player
                                         .into[GamePlayerEntity]
                                         .withFieldConst(_.playerOrder, maxOrder + 1)
                                         .withFieldConst(_.gameId, gameId)
                                         .withFieldRenamed(_.id, _.playerId)
                                         .transform
                                     )
                                   )
                               )
                             )
          } yield ()
        }

      override def getParticipantInfo(gameId: FUUID, playerId: FUUID): ZIO[Connection, DbException, Option[Player]] =
        tzio {
          run(
            quote {
              gamePlayers.filter(player => player.gameId == lift(gameId) && player.playerId == lift(playerId))
            }
          ).map(_.headOption.map(_.into[Player].withFieldRenamed(_.playerId, _.id).transform))
        }

      override def getParticipants(gameId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, List[Player]] =
        tzio {
          run(
            quote(
              gamePlayers.filter(player => player.gameId == lift(gameId)).sortBy(_.playerOrder)(Ord.asc)
            )
          ).map(_.map(_.into[Player].withFieldRenamed(_.playerId, _.id).transform))
        }

      override def getAcceptedParticipants(gameId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, List[Player]] =
        tzio {
          run(
            quote(
              gamePlayers.filter(player => player.gameId == lift(gameId) && player.role.isDefined).sortBy(_.playerOrder)(Ord.asc)
            )
          ).map(_.map(_.into[Player].withFieldRenamed(_.playerId, _.id).transform))
        }

      override def getPlayers(gameId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, List[Player]] =
        tzio {
          run(
            quote(
              gamePlayers
                .filter(player => player.gameId == lift(gameId) && player.role.getOrNull == lift(PlayerRole.Player: PlayerRole))
                .sortBy(_.playerOrder)(Ord.asc)
            )
          )

        }.map(_.map(_.into[Player].withFieldRenamed(_.playerId, _.id).transform))

      override def removePlayer(gameId: FUUID, playerId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, Boolean] =
        tzio {
          run {
            quote {
              gamePlayers.filter(player => player.playerId == lift(playerId) && player.gameId == lift(gameId)).delete
            }
          }.map(_ =!= 0L)
        }

      override def removePlayers(gameId: FUUID, playerIds: List[FUUID]): ZIO[Has[transactor.Transactor[Task]], DbException, Boolean] =
        tzio {
          run(
            quote(
              gamePlayers.filter(p => liftQuery(playerIds).contains(p.playerId)).delete
            )
          ).map(result => result > 0)
        }

      override def assignRole(
        gameId: FUUID,
        playerId: FUUID,
        role: PlayerRole
      ): ZIO[Has[transactor.Transactor[Task]], DbException, Boolean] =
        tzio {
          run(
            quote(
              gamePlayers
                .filter(player => player.gameId == lift(gameId) && player.playerId == lift(playerId))
                .update(_.role -> lift(Option(role)))
            )
          ).map(_ =!= 0L)
        }

      override def increaseTricksTaken(
        gameId: FUUID,
        playerId: FUUID,
        increaseBy: Index
      ): ZIO[Has[transactor.Transactor[Task]], DbException, Unit] =
        tzio {
          run(
            quote(
              gamePlayers
                .filter(p => p.gameId == lift(gameId) && p.playerId == lift(playerId))
                .update(p => p.takenTricks -> (p.takenTricks + lift(increaseBy)))
            )
          )
        }.unit

      override def getPlayersTricks(gameId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, Map[FUUID, Int]] =
        tzio {
          run(
            quote(
              gamePlayers.filter(p => p.gameId == lift(gameId) && p.role == lift(Some(PlayerRole.Player): Option[PlayerRole]))
            )
          ).map(_.map(player => player.playerId -> player.takenTricks).toMap)
        }

      private val games = quote {
        querySchema[GameEntity]("games")
      }

      private val gamePlayers = quote {
        querySchema[GamePlayerEntity]("game_players")
      }

    }
  }

}

private final case class GameEntity(
  id: FUUID,
  description: Option[String],
  creator: FUUID,
  startedAt: Option[Instant],
  finishedAt: Option[Instant],
  createdAt: Instant,
  updatedAt: Instant)

private final case class GamePlayerEntity(
  gameId: FUUID,
  playerId: FUUID,
  nickname: String,
  role: Option[PlayerRole],
  playerOrder: Int,
  takenTricks: Int = 0)
