package com.github.pawelj_pl.eoponline.config

import com.github.pawelj_pl.eoponline.config.AppConfig.MessageBroker
import zio.Layer
import zio.config._
import zio.config.magnolia.DeriveConfigDescriptor.descriptor
import zio.config.typesafe.TypesafeConfig.fromDefaultLoader

final case class AppConfig(server: AppConfig.Server, auth: AppConfig.Auth, database: AppConfig.Database, messageBroker: MessageBroker)

object AppConfig {

  final case class Server(port: Int, host: String)

  final case class Auth(secretKey: String)

  final case class Database(url: String, userName: String, password: String, maxPoolSize: Int)

  sealed trait MessageBroker

  object MessageBroker {

    case object InMemory extends MessageBroker

    case class Jms(url: String, username: String, password: String, destinations: Jms.Destinations) extends MessageBroker

    object Jms {
      final case class Destinations(websocketTopicName: String)
    }

  }

  private val configDescriptor = descriptor[AppConfig].mapKey(toKebabCase)

  val live: Layer[ReadError[String], ZConfig[AppConfig]] = fromDefaultLoader(configDescriptor)

}
