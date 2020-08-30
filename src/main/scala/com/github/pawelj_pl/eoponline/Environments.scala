package com.github.pawelj_pl.eoponline

import com.github.pawelj_pl.eoponline.config.AppConfig
import io.github.gaelrenoux.tranzactio.doobie.{Database => DoobieDb}
import com.github.pawelj_pl.eoponline.database.{DataSourceLayers, Database}
import com.github.pawelj_pl.eoponline.database.Database.Database
import com.github.pawelj_pl.eoponline.eventbus.TopicLayer
import com.github.pawelj_pl.eoponline.eventbus.handlers.WebSocketHandler
import com.github.pawelj_pl.eoponline.eventbus.handlers.WebSocketHandler.WebSocketHandler
import com.github.pawelj_pl.eoponline.game.{GameRoutes, Games}
import com.github.pawelj_pl.eoponline.game.GameRoutes.GameRoutes
import com.github.pawelj_pl.eoponline.game.repository.GamesRepository
import com.github.pawelj_pl.eoponline.http.websocket.{WebSocketRoutes, WebSocketTopicLayer}
import com.github.pawelj_pl.eoponline.http.websocket.WebSocketRoutes.WebSocketRoutes
import com.github.pawelj_pl.eoponline.session.UserRoutes.UserRoutes
import com.github.pawelj_pl.eoponline.session.{Authentication, Jwt, UserRoutes}
import com.github.pawelj_pl.eoponline.utils.RandomUtils
import zio.ZLayer
import zio.blocking.Blocking
import zio.clock.Clock
import zio.config.ZConfig
import zio.config.syntax._
import zio.logging.slf4j.Slf4jLogger
import zio.random.Random

object Environments {

  type HttpServerEnvironment = Database with GameRoutes with ZConfig[AppConfig.Server] with WebSocketHandler with WebSocketRoutes with UserRoutes

  val httpServerEnvironment: ZLayer[Any, Throwable, HttpServerEnvironment] = {
    val dbConfig = AppConfig.live.narrow(_.database)
    val database = dbConfig >>> Database.live
    val logging = Slf4jLogger.make((_, str) => str)
    val serverConfig = AppConfig.live.narrow(_.server)
    val authConfig = AppConfig.live.narrow(_.auth)
    val messageTopic = TopicLayer.live
    val webSocketTopic = WebSocketTopicLayer.live
    val clock = Clock.live
    val blocking = Blocking.live
    val random = Random.live
    val randomUtils = RandomUtils.live
    val dataSource = blocking ++ AppConfig.live.narrow(_.database) >>> DataSourceLayers.hikari
    val db = dataSource ++ blocking ++ clock >>> DoobieDb.fromDatasource
    val gamesRepo = clock >>> GamesRepository.postgres
    val games = gamesRepo ++ db ++ logging ++ messageTopic ++ randomUtils >>> Games.live
    val jwt = clock ++ random ++ authConfig >>> Jwt.live
    val authentication = clock ++ jwt ++ logging ++ randomUtils >>> Authentication.live
    val gameRoutes = games ++ authentication ++ logging >>> GameRoutes.live
    val webSocketHandler = logging ++ messageTopic ++ webSocketTopic ++ db ++ gamesRepo >>> WebSocketHandler.live
    val webSocketRoutes = webSocketTopic ++ authentication ++ logging >>> WebSocketRoutes.live
    val userRoutes = authentication >>> UserRoutes.live
    serverConfig ++ gameRoutes ++ database ++ webSocketHandler ++ webSocketRoutes ++ userRoutes
  }

}
