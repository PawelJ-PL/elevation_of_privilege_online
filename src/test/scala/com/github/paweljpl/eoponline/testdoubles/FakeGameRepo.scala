package com.github.paweljpl.eoponline.testdoubles

import java.time.Instant

import cats.syntax.eq._
import com.github.pawelj_pl.eoponline.game.repository.GamesRepository
import com.github.pawelj_pl.eoponline.game.{Game, Player, PlayerRole}
import doobie.util.transactor
import io.chrisdavenport.fuuid.FUUID
import io.github.gaelrenoux.tranzactio.DbException
import zio.{Has, Ref, Task, ULayer, ZIO, ZLayer}

object FakeGameRepo {

  case class GamesRepoState(games: Set[Game] = Set.empty, players: Map[FUUID, Set[Player]] = Map.empty)

  def withState(ref: Ref[GamesRepoState]): ULayer[Has[GamesRepository.Service]] =
    ZLayer.succeed(new GamesRepository.Service {

      override def findById(gameId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, Option[Game]] =
        ref.get.map(_.games.find(_.id === gameId))

      override def getActiveGame(gameId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, Option[Game]] = ???

      override def create(game: Game): ZIO[Has[transactor.Transactor[Task]], DbException, Game] =
        ref.update(prev => prev.copy(games = prev.games + game)).map(_ => game)

      override def setStartTime(gameId: FUUID, time: Instant): ZIO[Has[transactor.Transactor[Task]], DbException, Unit] =
        ref.update { state =>
          val old = state.games.find(_.id === gameId)
          val updated: Set[Game] = old
            .map(game => (state.games - game) + Game(gameId, game.description, game.creator, Some(time), game.finishedAt))
            .getOrElse(state.games)
          state.copy(games = updated)
        }

      override def addPlayer(gameId: FUUID, player: Player): ZIO[Has[transactor.Transactor[Task]], DbException, Unit] =
        ref.update { prev =>
          val players = prev.players.updatedWith(gameId) {
            case Some(value) => Some(value + player)
            case None        => Some(Set(player))
          }
          prev.copy(players = players)
        }

      override def getParticipantInfo(gameId: FUUID, playerId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, Option[Player]] =
        ref.get.map(_.players.get(gameId).flatMap(_.find(_.id === playerId)))

      override def getParticipants(gameId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, List[Player]] =
        ref.get.map(_.players.getOrElse(gameId, Set.empty).toList)

      override def getAcceptedParticipants(gameId: FUUID): ZIO[Has[transactor.Transactor[Task]], DbException, List[Player]] =
        getParticipants(gameId).map(_.filter(_.role.isDefined))

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
          .map(_ => true)

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
          .map(_ => true)

      override def assignRole(
        gameId: FUUID,
        playerId: FUUID,
        role: PlayerRole
      ): ZIO[Has[transactor.Transactor[Task]], DbException, Boolean] =
        ref
          .update { prev =>
            val players = prev.players.updatedWith(gameId) {
              case Some(value) =>
                val existingPlayer = value.find(_.id === playerId)
                val updated = existingPlayer.map(p => value - p + p.copy(role = Some(role))).getOrElse(value)
                Some(updated)
              case None        => None
            }
            prev.copy(players = players)
          }
          .map(_ => true)

    })

}
