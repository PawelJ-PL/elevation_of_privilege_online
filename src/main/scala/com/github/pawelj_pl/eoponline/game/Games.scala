package com.github.pawelj_pl.eoponline.game

import cats.syntax.eq._
import cats.syntax.show._
import com.github.pawelj_pl.eoponline.eventbus.InternalMessage
import com.github.pawelj_pl.eoponline.game.dto.{GameInfoSummary, NewGameDto}
import com.github.pawelj_pl.eoponline.game.repository.GamesRepository
import com.github.pawelj_pl.eoponline.game.repository.GamesRepository.GamesRepository
import com.github.pawelj_pl.eoponline.`match`.{CardLocation, Suit, Value}
import com.github.pawelj_pl.eoponline.`match`.repository.CardsRepository.CardsRepository
import com.github.pawelj_pl.eoponline.`match`.repository.{CardsRepository, GameplayRepository}
import com.github.pawelj_pl.eoponline.`match`.repository.GameplayRepository.GameplayRepository
import com.github.pawelj_pl.eoponline.utils.RandomUtils
import com.github.pawelj_pl.eoponline.utils.RandomUtils.RandomUtils
import com.github.pawelj_pl.eoponline.utils.CollectionsUtils.syntax._
import fs2.concurrent.Topic
import io.chrisdavenport.fuuid.FUUID
import io.github.gaelrenoux.tranzactio.doobie.Database
import zio.clock.Clock
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

    def startGameAs(gameId: FUUID, userId: FUUID): ZIO[Any, StartGameError, Unit]

    def getAllGamesOfUser(userId: FUUID): ZIO[Any, Nothing, List[GameInfoSummary]]

    def deleteGameAs(gameId: FUUID, userId: FUUID): ZIO[Any, DeleteGameError, Unit]

  }

  val live: ZLayer[GamesRepository with Has[Database.Service] with Logging with Has[
    Topic[Task, InternalMessage]
  ] with RandomUtils with GameplayRepository with CardsRepository with Clock, Nothing, Games] =
    ZLayer.fromServices[GamesRepository.Service, Database.Service, Logger[String], Topic[
      Task,
      InternalMessage
    ], RandomUtils.Service, GameplayRepository.Service, CardsRepository.Service, Clock.Service, Games.Service] {
      (gamesRepo, db, logger, topic, random, gamePlayRepo, cardsRepo, clock) =>
        new Service {

          override def create(dto: NewGameDto, creator: FUUID): ZIO[Any, Nothing, Game] =
            db.transactionOrDie(for {
              gameId <- random.randomFuuid
              game = Game(gameId, dto.description, creator, None, None)
              saved  <- gamesRepo.create(game)
              _      <- gamesRepo.addPlayer(saved.id, Player(saved.creator, dto.ownerNickname, Some(PlayerRole.Player)))
              _      <- logger.info(s"User $creator created game ${saved.id}")
            } yield saved)
              .orDie
              .tap(game => topic.publish1(InternalMessage.GameCreated(game)).orDie)

          override def getInfoAs(gameId: FUUID, userId: FUUID): ZIO[Any, GameInfoError, Game] =
            db.transactionOrDie(for {
              game   <- gamesRepo.findById(gameId).orDie.someOrFail(GameNotFound(gameId))
              player <- gamesRepo
                          .getParticipantInfo(gameId, userId)
                          .orDie
                          .someOrFail(game.startedAt match {
                            case Some(_) => GameAlreadyStarted(gameId)
                            case None    => ParticipantIsNotAMember(userId, gameId)
                          })
              _      <- ZIO.cond(player.role.isDefined, (): Unit, ParticipantNotAccepted(gameId, player))
            } yield game)

          override def joinGame(gameId: FUUID, userId: FUUID, nickName: String): ZIO[Any, JoinGameError, Unit] = {
            db.transactionOrDie(for {
              game <- gamesRepo.findById(gameId).orDie.someOrFail(GameNotFound(gameId))
              _    <- ZIO.cond(game.startedAt.isEmpty, (), GameAlreadyStarted(gameId))
              _    <- ZIO.cond(game.finishedAt.isEmpty, (), GameAlreadyFinished(gameId))
              _    <- gamesRepo.getParticipantInfo(gameId, userId).orDie.flatMap {
                        case Some(value) => ZIO.fail(ParticipantAlreadyJoined(gameId, value))
                        case None        => ZIO.unit
                      }
              _    <- gamesRepo.addPlayer(gameId, Player(userId, nickName, None)).orDie
              _    <- logger.info(s"User $userId joined to game $gameId")
            } yield ())
          } *> topic.publish1(InternalMessage.ParticipantJoined(gameId, userId, nickName)).orDie

          override def getParticipantsAs(gameId: FUUID, userId: FUUID): ZIO[Any, GetParticipantsError, List[Player]] =
            db.transactionOrDie(for {
              participants <- gamesRepo.getParticipants(gameId).orDie
              maybePlayer = participants.find(player => player.id == userId)
              _            <- maybePlayer match {
                                case Some(value) => ZIO.cond(value.role.isDefined, (), ParticipantNotAccepted(gameId, value))
                                case None        => ZIO.fail(ParticipantIsNotAMember(userId, gameId))
                              }
            } yield participants)

          override def kickUserAs(gameId: FUUID, userToKick: FUUID, performActionAs: FUUID): ZIO[Any, KickUserError, Unit] = {
            db.transactionOrDie(for {
              _    <- ZIO.cond(userToKick =!= performActionAs, (), KickSelfForbidden(gameId, userToKick))
              game <- gamesRepo.findById(gameId).orDie
              _    <- game match {
                        case Some(value) if value.creator =!= performActionAs => ZIO.fail(UserIsNotGameOwner(gameId, performActionAs))
                        case Some(value) if value.startedAt.isDefined         => ZIO.fail(GameAlreadyStarted(value.id))
                        case Some(value) if value.finishedAt.isDefined        => ZIO.fail(GameAlreadyFinished(value.id))
                        case Some(_)                                          => ZIO.unit
                        case None                                             => ZIO.fail(GameNotFound(gameId))
                      }
              _    <- gamesRepo.removePlayer(gameId, userToKick).orDie *>
                        logger.info(show"User $performActionAs kicked user $userToKick from game $gameId")
            } yield ())
          } *> topic.publish1(InternalMessage.ParticipantKicked(gameId, userToKick)).orDie

          override def assignRoleAs(
            gameId: FUUID,
            userToUpdate: FUUID,
            role: PlayerRole,
            performActionAs: FUUID
          ): ZIO[Any, AssignRoleError, Unit] = {
            db.transactionOrDie(
              for {
                game <- gamesRepo.findById(gameId).orDie
                _    <- game match {
                          case Some(value) if value.creator =!= performActionAs => ZIO.fail(UserIsNotGameOwner(gameId, performActionAs))
                          case Some(value) if value.startedAt.isDefined         => ZIO.fail(GameAlreadyStarted(gameId))
                          case Some(value) if value.finishedAt.isDefined        => ZIO.fail(GameAlreadyFinished(gameId))
                          case Some(_)                                          => ZIO.unit
                          case None                                             => ZIO.fail(GameNotFound(gameId))
                        }
                _    <- gamesRepo.assignRole(gameId, userToUpdate, role).orDie *>
                          logger.info(s"User $performActionAs changed role of user $userToUpdate in game $gameId to $role")
              } yield ()
            )
          } *> topic.publish1(InternalMessage.RoleAssigned(gameId, userToUpdate, role)).orDie

          override def startGameAs(gameId: FUUID, userId: FUUID): ZIO[Any, StartGameError, Unit] =
            db.transactionOrDie(
              for {
                game         <- gamesRepo.findById(gameId).orDie
                _            <- game match {
                                  case Some(game) => validateGameStart(game, userId)
                                  case None       => ZIO.fail(GameNotFound(gameId))
                                }
                participants <- gamesRepo.getParticipants(gameId).orDie
                players = participants.filter(_.role.exists(_ === PlayerRole.Player))
                _            <- ZIO.cond(players.size > 1, (), NotEnoughPlayers(gameId, players.size))
                _            <- ZIO.cond(players.size <= 10, (), TooManyPlayers(gameId, players.size))
                deck         <- random.shuffle((1 to 74).toList).map(_.distribute(players.size).map(_.toList))
                hands = players.zip(deck)
                _            <- logger.info(s"Initializing deck for game $gameId")
                _            <- gamePlayRepo.initDeck(gameId, hands).orDie
                firstCard    <- cardsRepo
                                  .get(Value.Three, Suit.Tampering)
                                  .someOrFail(new RuntimeException("Card not found"))
                                  .orDie
                firstPlayer  <- ZIO
                                  .fromOption(hands.find(_._2.contains(firstCard.cardNumber)))
                                  .bimap(_ => new Throwable("Player with first card not found"), _._1)
                                  .orDie
                _            <- gamePlayRepo.updateState(gameId, firstPlayer.id, Some(Suit.Tampering)).orDie
                _            <- gamePlayRepo.updateCardLocation(gameId, firstCard.cardNumber, CardLocation.Table).orDie
                now          <- clock.instant
                _            <- gamesRepo.setStartTime(gameId, now).orDie
                usersToRemove = participants.filter(_.role.isEmpty).map(_.id)
                _            <- logger.info(s"Removing not accepted participants $usersToRemove")
                _            <- gamesRepo.removePlayers(gameId, usersToRemove).orDie
                _            <- ZIO.foreach_(usersToRemove) { id =>
                                  topic
                                    .publish1(InternalMessage.ParticipantKicked(gameId, id))
                                    .catchAll(err => logger.throwable("Unable to send internal message", err))
                                }
                _            <- topic
                                  .publish1(InternalMessage.GameStarted(gameId))
                                  .catchAll(err => logger.throwable("Unable to send internal message", err))
                _            <- logger.info(show"Game $gameId started")
              } yield ()
            )

          private def validateGameStart(game: Game, userId: FUUID): ZIO[Any, StartGameError, Unit] =
            if (game.creator =!= userId)
              ZIO.fail(UserIsNotGameOwner(game.id, userId))
            else if (game.startedAt.isDefined)
              ZIO.fail(GameAlreadyStarted(game.id))
            else ZIO.fail(GameAlreadyFinished(game.id)).when(game.finishedAt.isDefined)

          override def getAllGamesOfUser(userId: FUUID): ZIO[Any, Nothing, List[GameInfoSummary]] =
            db.transactionOrDie(gamesRepo.getGamesInfoByUser(userId).orDie)

          override def deleteGameAs(gameId: FUUID, userId: FUUID): ZIO[Any, DeleteGameError, Unit] = {
            db.transactionOrDie(
              for {
                game <- gamesRepo.findById(gameId).orDie.someOrFail(GameNotFound(gameId))
                _    <- ZIO.fail(UserIsNotGameOwner(gameId, userId)).unless(game.creator === userId)
                _    <- logger.info(show"User $userId is going to remove game $gameId")
                _    <- gamesRepo.deleteGames(List(gameId)).orDie
              } yield ()
            )
          } *> topic.publish1(InternalMessage.GameDeleted(gameId)).orDie
        }

    }

}
