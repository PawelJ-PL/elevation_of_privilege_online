package com.github.pawelj_pl.eoponline.scheduler

import cats.instances.string._
import cats.syntax.show._
import com.github.pawelj_pl.eoponline.config.AppConfig
import com.github.pawelj_pl.eoponline.game.repository.GamesRepository
import com.github.pawelj_pl.eoponline.game.repository.GamesRepository.GamesRepository
import io.github.gaelrenoux.tranzactio.doobie.Database
import zio.clock.Clock
import zio.logging.{Logger, Logging}
import zio.{Has, Schedule, ZIO, ZLayer}

object GameCleanup {

  type GameCleanup = Has[GameCleanup.Service]

  trait Service {

    val schedule: ZIO[Any, Nothing, Unit]

  }

  val schedule: ZIO[GameCleanup, Nothing, Unit] = ZIO.accessM[GameCleanup](_.get.schedule)

  val live: ZLayer[Clock with Has[AppConfig.Schedulers.GameCleaner] with GamesRepository with Has[
    Database.Service
  ] with Logging, Nothing, GameCleanup] =
    ZLayer.fromServices[Clock.Service, AppConfig.Schedulers.GameCleaner, GamesRepository.Service, Database.Service, Logger[
      String
    ], GameCleanup.Service] { (clock, schedulerConfig, gamesRepo, db, logger) =>
      new Service {
        private val cleanup: ZIO[Clock, Throwable, Unit] = db
          .transactionOrDie(
            for {
              now           <- clock.instant
              outdatedGames <- gamesRepo.getGamesOlderThan(now.minus(schedulerConfig.gameValidFor))
              formattedIds = outdatedGames.map(_.id.show).mkString(", ")
              _             <- logger.info(show"Outdated games will be removed: $formattedIds").unless(outdatedGames.isEmpty)
              _             <- gamesRepo.deleteGames(outdatedGames.map(_.id))
            } yield ()
          )
          .resurrect

        private val scheduler = Schedule.forever && Schedule.windowed(schedulerConfig.runEvery)

        override val schedule: ZIO[Any, Nothing, Unit] =
          cleanup
            .catchAll(err => logger.throwable("Unable to cleanup games", err))
            .repeat(scheduler)
            .provideLayer(ZLayer.succeed(clock))
            .unit

      }
    }

}
