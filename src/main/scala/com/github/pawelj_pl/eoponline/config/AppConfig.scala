package com.github.pawelj_pl.eoponline.config

import zio.Layer
import zio.config._
import zio.config.magnolia.DeriveConfigDescriptor.descriptor
import zio.config.typesafe.TypesafeConfig.fromDefaultLoader

final case class AppConfig(server: AppConfig.Server, auth: AppConfig.Auth, database: AppConfig.Database)

object AppConfig {

  final case class Server(port: Int, host: String)

  final case class Auth(secretKey: String)

  final case class Database(url: String, userName: String, password: String, maxPoolSize: Int)

  private val configDescriptor = descriptor[AppConfig].mapKey(toKebabCase)

  val live: Layer[ReadError[String], ZConfig[AppConfig]] = fromDefaultLoader(configDescriptor)

}
