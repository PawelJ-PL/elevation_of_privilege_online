package com.github.paweljpl.eoponline.testdoubles

import java.time.Instant
import cats.instances.option._
import cats.syntax.eq._
import com.github.pawelj_pl.eoponline.game.dto.GameInfoSummary
import com.github.pawelj_pl.eoponline.game.repository.GamesRepository
import com.github.pawelj_pl.eoponline.game.{Game, Player, PlayerRole}
import doobie.util.transactor
import io.chrisdavenport.fuuid.FUUID
import io.github.gaelrenoux.tranzactio.DbException
import zio.{Has, Ref, Task, ULayer, ZIO, ZLayer}

object FakeGameRepo {

  final case class PlayerTricks(playerId: FUUID, gameId: FUUID, tricks: Int)

  final case class GamesRepoState(
    games: Set[Game] = Set.empty,
    players: Map[FUUID, List[Player]] = Map.empty,
    tricksByPlayer: Set[PlayerTricks] = Set.empty)

  def withState(ref: Ref[GamesRepoState]): ULayer[Has[GamesRepository.Service]] =
    ZLayer.succeed(new GamesRepository.Service {

      override def findById(gameId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, Option[Game]] =
        ref.get.map(_.games.find(_.id === gameId))

      override def getGamesInfoByUser(userId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, List[GameInfoSummary]] = ???

      override def create(game: Game): ZIO[Has[transactor.Transactor[Task]], DbException, Game] =
        ref.update(prev => prev.copy(games = prev.games + game)).as(game)

      override def setStartTime(gameId: FUUID, time: Instant): ZIO[Has[transactor.Transactor[Task]], DbException, Unit] =
        ref.update { state =>
          val old = state.games.find(_.id === gameId)
          val updated: Set[Game] = old
            .map(game => (state.games - game) + Game(gameId, game.description, game.creator, Some(time), game.finishedAt))
            .getOrElse(state.games)
          state.copy(games = updated)
        }

      override def setFinishTime(gameId: FUUID, time: Instant): ZIO[Has[transactor.Transactor[Task]], DbException, Unit] =
        ref.update { state =>
          val old = state.games.find(_.id === gameId)
          val updated: Set[Game] = old
            .map(game => (state.games - game) + Game(gameId, game.description, game.creator, game.startedAt, Some(time)))
            .getOrElse(state.games)
          state.copy(games = updated)
        }

      override def getGamesOlderThan(time: Instant): ZIO[Has[transactor.Transactor[Task]], DbException, List[Game]] = ???

      override def deleteGames(gameIds: List[FUUID]): ZIO[Has[transactor.Transactor[Task]], DbException, Long] =
        for {
          prev    <- ref.get
          updated <- ref.updateAndGet { state =>
                       val updatedGames = state.games.filter(game => !gameIds.contains(game.id))
                       state.copy(games = updatedGames)
                     }
        } yield prev.games.size - updated.games.size

      override def addPlayer(gameId: FUUID, player: Player): ZIO[Has[transactor.Transactor[Task]], DbException, Unit] =
        ref.update { prev =>
          val players = prev.players.updatedWith(gameId) {
            case Some(value) => Some(value :+ player)
            case None        => Some(List(player))
          }
          prev.copy(players = players)
        }

      override def getParticipantInfo(gameId: FUUID, playerId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, Option[Player]] =
        ref.get.map(_.players.get(gameId).flatMap(_.find(_.id === playerId)))

      override def getParticipants(gameId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, List[Player]] =
        ref.get.map(_.players.getOrElse(gameId, Set.empty).toList)

      override def getAcceptedParticipants(gameId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, List[Player]] =
        getParticipants(gameId).map(_.filter(_.role.isDefined))

      override def getPlayers(gameId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, List[Player]] =
        ref.get.map(_.players.getOrElse(gameId, List.empty).filter(_.role === Some(PlayerRole.Player)))

      override def removePlayer(gameId: FUUID, playerId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, Boolean] =
        ref
          .update { prev =>
            val players = prev.players.updatedWith(gameId) {
              case Some(value) =>
                Some(value.filter(_.id =!= playerId))
              case None        => None
            }
            prev.copy(players = players)
          }
          .as(true)

      override def removePlayers(gameId: FUUID, playerIds: List[FUUID]): ZIO[Has[transactor.Transactor[Task]], DbException, Boolean] =
        ref
          .update { prev =>
            val players = prev.players.updatedWith(gameId) {
              case Some(value) =>
                Some(value.filter(p => !playerIds.contains(p.id)))
              case None        => None
            }
            prev.copy(players = players)
          }
          .as(true)

      override def assignRole(
        gameId: FUUID,
        playerId: FUUID,
        role: PlayerRole
      ): ZIO[Has[transactor.Transactor[Task]], DbException, Boolean] =
        ref
          .update { prev =>
            val players = prev.players.updatedWith(gameId) {
              case Some(value) =>
                val updated = value.map(p => if (p.id === playerId) p.copy(role = Some(role)) else p)
                Some(updated)
              case None        => None
            }
            prev.copy(players = players)
          }
          .as(true)

      override def increaseTricksTaken(
        gameId: FUUID,
        playerId: FUUID,
        increaseBy: Int
      ): ZIO[Has[transactor.Transactor[Task]], DbException, Unit] =
        ref.update { prev =>
          val playersTricks = prev.tricksByPlayer.find(t => t.gameId === gameId && t.playerId === playerId)
          val updated = playersTricks match {
            case Some(value) => prev.tricksByPlayer - value + value.copy(tricks = value.tricks + 1)
            case None        => prev.tricksByPlayer + PlayerTricks(playerId, gameId, 1)
          }
          prev.copy(tricksByPlayer = updated)
        }

      override def getPlayersTricks(gameId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, Map[FUUID, Int]] =
        ref.get.map(_.tricksByPlayer.filter(_.gameId === gameId).map(trick => trick.playerId -> trick.tricks).toMap)

    })

}
