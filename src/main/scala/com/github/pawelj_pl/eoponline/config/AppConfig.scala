package com.github.pawelj_pl.eoponline.config

import java.time.Duration
import com.github.pawelj_pl.eoponline.config.AppConfig.{MessageBroker, Schedulers}
import zio.{Has, Layer}
import zio.config._
import zio.config.magnolia.DeriveConfigDescriptor.descriptor
import zio.config.typesafe.TypesafeConfig.fromDefaultLoader

final case class AppConfig(
  server: AppConfig.Server,
  auth: AppConfig.Auth,
  database: AppConfig.Database,
  messageBroker: MessageBroker,
  schedulers: Schedulers)

object AppConfig {

  final case class Server(port: Int, host: String)

  final case class Auth(secretKey: String)

  final case class Database(url: String, userName: String, password: String, maxPoolSize: Int)

  sealed trait MessageBroker

  object MessageBroker {

    final case object InMemory extends MessageBroker

    final case class Jms(url: String, username: String, password: String, destinations: Jms.Destinations) extends MessageBroker

    object Jms {

      final case class Destinations(websocketTopicName: String)

    }

  }

  final case class Schedulers(gameCleaner: Schedulers.GameCleaner)

  object Schedulers {

    final case class GameCleaner(runEvery: Duration, gameValidFor: Duration)

  }

  private val configDescriptor = descriptor[AppConfig].mapKey(toKebabCase)

  val live: Layer[ReadError[String], Has[AppConfig]] = fromDefaultLoader(configDescriptor)

}
