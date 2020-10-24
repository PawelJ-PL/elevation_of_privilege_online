package com.github.pawelj_pl.eoponline

import com.gh.dobrynya.zio.jms.{BlockingConnection, Topic, connection}
import com.github.pawelj_pl.eoponline.config.AppConfig
import com.github.pawelj_pl.eoponline.config.AppConfig.MessageBroker
import io.github.gaelrenoux.tranzactio.doobie.{Database => DoobieDb}
import com.github.pawelj_pl.eoponline.database.{DataSourceLayers, Database}
import com.github.pawelj_pl.eoponline.database.Database.Database
import com.github.pawelj_pl.eoponline.eventbus.TopicLayer
import com.github.pawelj_pl.eoponline.eventbus.broker.MessageTopic
import com.github.pawelj_pl.eoponline.eventbus.broker.MessageTopic.MessageTopic
import com.github.pawelj_pl.eoponline.eventbus.handlers.WebSocketHandler
import com.github.pawelj_pl.eoponline.eventbus.handlers.WebSocketHandler.WebSocketHandler
import com.github.pawelj_pl.eoponline.game.{GameRoutes, Games}
import com.github.pawelj_pl.eoponline.game.GameRoutes.GameRoutes
import com.github.pawelj_pl.eoponline.game.repository.GamesRepository
import com.github.pawelj_pl.eoponline.`match`.{MatchRoutes, Matches}
import com.github.pawelj_pl.eoponline.`match`.MatchRoutes.MatchRoutes
import com.github.pawelj_pl.eoponline.`match`.repository.{CardsRepository, GameplayRepository}
import com.github.pawelj_pl.eoponline.http.web.StaticRoutes
import com.github.pawelj_pl.eoponline.http.web.StaticRoutes.StaticRoutes
import com.github.pawelj_pl.eoponline.http.websocket.{WebSocketMessage, WebSocketRoutes}
import com.github.pawelj_pl.eoponline.http.websocket.WebSocketRoutes.WebSocketRoutes
import com.github.pawelj_pl.eoponline.session.UserRoutes.UserRoutes
import com.github.pawelj_pl.eoponline.session.{Authentication, Jwt, UserRoutes}
import com.github.pawelj_pl.eoponline.utils.RandomUtils
import javax.jms.{ConnectionFactory, JMSException}
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory
import zio.{Has, ZLayer}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.config.{ZConfig, getConfig}
import zio.config.syntax._
import zio.logging.{Logger, Logging}
import zio.logging.slf4j.Slf4jLogger
import zio.random.Random

object Environments {

  type HttpServerEnvironment = Database
    with GameRoutes
    with ZConfig[AppConfig.Server]
    with WebSocketHandler
    with WebSocketRoutes
    with UserRoutes
    with MatchRoutes
    with StaticRoutes

  private val provideWebSocketTopic
    : ZLayer[ZConfig[MessageBroker] with Blocking with Logging, Throwable, MessageTopic[WebSocketMessage[_]]] =
    ZLayer
      .fromServiceManaged[Logger[String], Blocking with ZConfig[MessageBroker], Throwable, MessageTopic[WebSocketMessage[_]]](logger =>
        getConfig[AppConfig.MessageBroker].toManaged_.flatMap {
          case MessageBroker.InMemory       => MessageTopic.inMemory[WebSocketMessage[_]](WebSocketMessage.TopicStarted).build
          case jmsConfig: MessageBroker.Jms =>
            val connectionFactory: ConnectionFactory = new ActiveMQConnectionFactory(jmsConfig.url, jmsConfig.username, jmsConfig.password)
            val connectionLayer: ZLayer[Blocking, JMSException, Has[BlockingConnection]] = ZLayer.fromManaged {
              ZLayer.fromManaged(connection(connectionFactory)).passthrough.build
            }
            (connectionLayer ++ ZLayer.succeed(logger) >>> MessageTopic
              .jms[WebSocketMessage[_]](Topic(jmsConfig.destinations.websocketTopicName))).build
        }
      )
      .map(_.get)

  val httpServerEnvironment: ZLayer[Any, Throwable, HttpServerEnvironment] = {
    val dbConfig = AppConfig.live.narrow(_.database)
    val database = dbConfig >>> Database.live
    val logging = Slf4jLogger.make((_, str) => str)
    val serverConfig = AppConfig.live.narrow(_.server)
    val authConfig = AppConfig.live.narrow(_.auth)
    val blocking = Blocking.live
    val messageTopic = TopicLayer.live
    val brokerConfig = AppConfig.live.narrow(_.messageBroker)
    val webSocketTopic = brokerConfig ++ blocking ++ logging >>> provideWebSocketTopic
    val clock = Clock.live
    val random = Random.live
    val randomUtils = random >>> RandomUtils.live
    val dataSource = blocking ++ AppConfig.live.narrow(_.database) >>> DataSourceLayers.hikari
    val db = dataSource ++ blocking ++ clock >>> DoobieDb.fromDatasource
    val gamesRepo = clock >>> GamesRepository.postgres
    val gamePlayRepo = GameplayRepository.postgres
    val cardRepo = CardsRepository.fromJsonFile
    val games = gamesRepo ++ db ++ logging ++ messageTopic ++ randomUtils ++ gamePlayRepo ++ cardRepo ++ clock >>> Games.live
    val jwt = clock ++ random ++ authConfig >>> Jwt.live
    val authentication = clock ++ jwt ++ logging ++ randomUtils >>> Authentication.live
    val gameRoutes = games ++ authentication ++ logging >>> GameRoutes.live
    val webSocketHandler = logging ++ messageTopic ++ webSocketTopic ++ db ++ gamesRepo >>> WebSocketHandler.live
    val webSocketRoutes = webSocketTopic ++ authentication ++ logging >>> WebSocketRoutes.live
    val userRoutes = authentication >>> UserRoutes.live
    val matches = db ++ gamesRepo ++ gamePlayRepo ++ cardRepo >>> Matches.live
    val gameplayRoutes = authentication ++ matches ++ logging >>> MatchRoutes.live
    val staticRoutes = blocking >>> StaticRoutes.live
    serverConfig ++ gameRoutes ++ database ++ webSocketHandler ++ webSocketRoutes ++ userRoutes ++ gameplayRoutes ++ staticRoutes
  }

}
