package com.github.pawelj_pl.eoponline.http.websocket

import cats.syntax.eq._
import com.github.pawelj_pl.eoponline.http.websocket.WsFrameEncoder.instances._
import com.github.pawelj_pl.eoponline.session.Authentication.Authentication
import com.github.pawelj_pl.eoponline.session.{Authentication, Session}
import fs2.Pipe
import fs2.concurrent.Topic
import io.chrisdavenport.fuuid.FUUID
import io.chrisdavenport.fuuid.http4s.FUUIDVar
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import zio.interop.catz._
import zio.logging.{Logger, Logging}
import zio.{Has, Task, UIO, ZIO, ZLayer}

object WebSocketRoutes {

  type WebSocketRoutes = Has[WebSocketRoutes.Service]

  trait Service {

    def routes: UIO[HttpRoutes[Task]]

  }

  val routes: ZIO[WebSocketRoutes, Nothing, HttpRoutes[Task]] = ZIO.accessM[WebSocketRoutes](_.get.routes)

  val live: ZLayer[Has[Topic[Task, WebSocketMessage[_]]] with Authentication with Logging, Nothing, WebSocketRoutes] =
    ZLayer.fromServices[Topic[Task, WebSocketMessage[_]], Authentication.Service, Logger[String], WebSocketRoutes.Service] {
      (wsTopic, auth, logger) =>
        new Service with Http4sDsl[Task] {

          override def routes: UIO[HttpRoutes[Task]] = anteroomRoutes

          private val anteroomRoutes: ZIO[Any, Nothing, HttpRoutes[Task]] = auth.sessionMiddleware.map { sessionMiddleware =>
            val routesWithSession = AuthedRoutes.of[Session, Task] {
              case GET -> Root / "games" / FUUIDVar(gameId) / "anteroom" as session =>
                WebSocketBuilder[Task].build(anteroomToClient(gameId, session.userId), anteroomFromClient(gameId, session.userId)).resurrect
            }
            sessionMiddleware(routesWithSession)
          }

          private def anteroomFromClient(gameId: FUUID, userId: FUUID): Pipe[Task, WebSocketFrame, Unit] =
            _.evalMap {
              case WebSocketFrame.Text(text, _) => logger.info(s"Received frame $text")
              case f                            => logger.info(s"Unknown frame type: $f")
            }

          private def anteroomToClient(gameId: FUUID, userId: FUUID): fs2.Stream[Task, WebSocketFrame] =
            wsTopic
              .subscribe(10)
              .filter(m => verifyRecipient(m.recipient, gameId, userId))
              .collect {
                case m: WebSocketMessage.NewParticipant     => m.toFrame
                case m: WebSocketMessage.ParticipantRemoved => m.toFrame
                case m: WebSocketMessage.UserRoleChanged    => m.toFrame
              }

          private def verifyRecipient(recipient: MessageRecipient, gameId: FUUID, userId: FUUID): Boolean =
            recipient match {
              case MessageRecipient.SingleGameParticipant(game, user)     => game === gameId && user === userId
              case MessageRecipient.MultipleGameParticipants(game, users) => game === gameId && users.contains(userId)
              case MessageRecipient.Broadcast                             => true
            }
        }
    }

}
