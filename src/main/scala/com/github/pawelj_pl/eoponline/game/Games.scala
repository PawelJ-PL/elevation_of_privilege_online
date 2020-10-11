package com.github.pawelj_pl.eoponline.game

import cats.syntax.eq._
import cats.syntax.show._
import com.github.pawelj_pl.eoponline.eventbus.InternalMessage
import com.github.pawelj_pl.eoponline.game.dto.NewGameDto
import com.github.pawelj_pl.eoponline.game.repository.GamesRepository
import com.github.pawelj_pl.eoponline.game.repository.GamesRepository.GamesRepository
import com.github.pawelj_pl.eoponline.utils.RandomUtils
import com.github.pawelj_pl.eoponline.utils.RandomUtils.RandomUtils
import com.github.pawelj_pl.eoponline.utils.ZioUtils.implicits._
import fs2.concurrent.Topic
import io.chrisdavenport.fuuid.FUUID
import io.github.gaelrenoux.tranzactio.doobie.Database
import zio.{Has, Task, ZIO, ZLayer}
import zio.logging.{Logger, Logging}

object Games {

  type Games = Has[Games.Service]

  trait Service {

    def create(dto: NewGameDto, creator: FUUID): ZIO[Any, Nothing, Game]

    def getInfoAs(gameId: FUUID, userId: FUUID): ZIO[Any, GameInfoError, Game]

    def joinGame(gameId: FUUID, userId: FUUID, nickName: String): ZIO[Any, JoinGameError, Unit]

    def getParticipantsAs(gameId: FUUID, userId: FUUID): ZIO[Any, GetParticipantsError, List[Player]]

    def kickUserAs(gameId: FUUID, userToKick: FUUID, performActionAs: FUUID): ZIO[Any, KickUserError, Unit]

    def assignRoleAs(gameId: FUUID, userToUpdate: FUUID, role: PlayerRole, performActionAs: FUUID): ZIO[Any, AssignRoleError, Unit]

  }

  val live: ZLayer[GamesRepository with Has[Database.Service] with Logging with Has[
    Topic[Task, InternalMessage]
  ] with RandomUtils, Nothing, Games] =
    ZLayer.fromServices[GamesRepository.Service, Database.Service, Logger[String], Topic[
      Task,
      InternalMessage
    ], RandomUtils.Service, Games.Service] { (repo, db, logger, topic, random) =>
      new Service {

        override def create(dto: NewGameDto, creator: FUUID): ZIO[Any, Nothing, Game] =
          db.transactionOrDie(for {
            gameId <- random.randomFuuid
            game = Game(gameId, dto.description, creator, None)
            saved  <- repo.create(game)
            _      <- repo.addPlayer(saved.id, Player(saved.creator, dto.ownerNickname, Some(PlayerRole.Player)))
            _      <- logger.info(s"User $creator created game ${saved.id}")
          } yield saved)
            .orDie
            .flatTap(game => topic.publish1(InternalMessage.GameCreated(game)).orDie)

        override def getInfoAs(gameId: FUUID, userId: FUUID): ZIO[Any, GameInfoError, Game] =
          db.transactionOrDie(for {
            game   <- repo.findById(gameId).orDie.someOrFail(GameNotFound(gameId))
            player <- repo.getParticipantInfo(gameId, userId).orDie.someOrFail(ParticipantIsNotAMember(userId, gameId))
            _      <- ZIO.cond(player.role.isDefined, (): Unit, ParticipantNotAccepted(gameId, player))
          } yield game)

        override def joinGame(gameId: FUUID, userId: FUUID, nickName: String): ZIO[Any, JoinGameError, Unit] =
          db.transactionOrDie(for {
            game <- repo.findById(gameId).orDie.someOrFail(GameNotFound(gameId))
            _    <- ZIO.cond(game.startedAt.isEmpty, (), GameAlreadyStarted(gameId))
            _    <- repo.getParticipantInfo(gameId, userId).orDie.flatMap {
                      case Some(value) => ZIO.fail(ParticipantAlreadyJoined(gameId, value))
                      case None        => ZIO.unit
                    }
            _    <- repo.addPlayer(gameId, Player(userId, nickName, None)).orDie
            _    <- logger.info(s"User $userId joined to game $gameId")
          } yield ())
            .flatMap(_ => topic.publish1(InternalMessage.ParticipantJoined(gameId, userId, nickName)).orDie)

        override def getParticipantsAs(gameId: FUUID, userId: FUUID): ZIO[Any, GetParticipantsError, List[Player]] =
          db.transactionOrDie(for {
            participants <- repo.getParticipants(gameId).orDie
            maybePlayer = participants.find(player => player.id == userId)
            _            <- maybePlayer match {
                              case Some(value) => ZIO.cond(value.role.isDefined, (), ParticipantNotAccepted(gameId, value))
                              case None        => ZIO.fail(ParticipantIsNotAMember(userId, gameId))
                            }
          } yield participants)

        override def kickUserAs(gameId: FUUID, userToKick: FUUID, performActionAs: FUUID): ZIO[Any, KickUserError, Unit] =
          db.transactionOrDie(for {
            _    <- ZIO.cond(userToKick =!= performActionAs, (), KickSelfForbidden(gameId, userToKick))
            game <- repo.findById(gameId).orDie
            _    <- game match {
                      case Some(value) if value.creator =!= performActionAs => ZIO.fail(UserIsNotGameOwner(gameId, performActionAs))
                      case Some(value) if value.startedAt.isDefined         => ZIO.fail(GameAlreadyStarted(value.id))
                      case Some(_)                                          => ZIO.unit
                      case None                                             => ZIO.fail(GameNotFound(gameId))
                    }
            _    <- repo.removePlayer(gameId, userToKick).orDie *>
                      logger.info(show"User $performActionAs kicked user $userToKick from game $gameId")
          } yield ())
            .flatMap(_ => topic.publish1(InternalMessage.ParticipantKicked(gameId, userToKick)).orDie)

        override def assignRoleAs(
          gameId: FUUID,
          userToUpdate: FUUID,
          role: PlayerRole,
          performActionAs: FUUID
        ): ZIO[Any, AssignRoleError, Unit] =
          db.transactionOrDie(
            for {
              game <- repo.findById(gameId).orDie
              _    <- game match {
                        case Some(value) if value.creator =!= performActionAs => ZIO.fail(UserIsNotGameOwner(gameId, performActionAs))
                        case Some(value) if value.startedAt.isDefined         => ZIO.fail(GameAlreadyStarted(gameId))
                        case Some(_)                                          => ZIO.unit
                        case None                                             => ZIO.fail(GameNotFound(gameId))
                      }
              _    <- repo.assignRole(gameId, userToUpdate, role).orDie *>
                        logger.info(s"User $performActionAs changed role of user $userToUpdate in game $gameId to $role")
            } yield ()
          ).flatMap(_ => topic.publish1(InternalMessage.RoleAssigned(gameId, userToUpdate, role)).orDie)
      }
    }

}
