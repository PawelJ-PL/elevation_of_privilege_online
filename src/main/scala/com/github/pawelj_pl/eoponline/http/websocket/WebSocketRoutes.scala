package com.github.pawelj_pl.eoponline.http.websocket

import cats.syntax.eq._
import com.github.pawelj_pl.eoponline.`match`.Matches
import com.github.pawelj_pl.eoponline.`match`.Matches.Matches
import com.github.pawelj_pl.eoponline.eventbus.broker.MessageTopic
import com.github.pawelj_pl.eoponline.eventbus.broker.MessageTopic.MessageTopic
import com.github.pawelj_pl.eoponline.http.websocket.WsFrameEncoder.instances._
import com.github.pawelj_pl.eoponline.session.Authentication.Authentication
import com.github.pawelj_pl.eoponline.session.{Authentication, Session}
import fs2.Pipe
import io.chrisdavenport.fuuid.FUUID
import io.chrisdavenport.fuuid.http4s.FUUIDVar
import org.http4s.{AuthedRoutes, HttpRoutes, Response, Status}
import org.http4s.blaze.pipeline.Command.{EOF => EOFCommand}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import zio.interop.catz._
import zio.logging.{Logger, Logging}
import zio.{Has, Task, UIO, ZIO, ZLayer}
import zio.stream.interop.fs2z._

object WebSocketRoutes {

  type WebSocketRoutes = Has[WebSocketRoutes.Service]

  trait Service {

    def routes: UIO[HttpRoutes[Task]]

  }

  val routes: ZIO[WebSocketRoutes, Nothing, HttpRoutes[Task]] = ZIO.accessM[WebSocketRoutes](_.get.routes)

  val live: ZLayer[MessageTopic[WebSocketMessage[_]] with Authentication with Logging with Matches, Nothing, WebSocketRoutes] =
    ZLayer.fromServices[MessageTopic.Service[WebSocketMessage[_]], Authentication.Service, Logger[
      String
    ], Matches.Service, WebSocketRoutes.Service] { (wsTopic, auth, logger, matches) =>
      new Service with Http4sDsl[Task] {

        override def routes: UIO[HttpRoutes[Task]] = anteroomRoutes

        private val anteroomRoutes: ZIO[Any, Nothing, HttpRoutes[Task]] = auth.sessionMiddleware.map { sessionMiddleware =>
          val routesWithSession = AuthedRoutes.of[Session, Task] {
            case GET -> Root / "games" / FUUIDVar(gameId) / "anteroom" as session =>
              WebSocketBuilder[Task].build(anteroomToClient(gameId, session.userId), fromClient(gameId, session.userId)).resurrect
            case GET -> Root / "games" / FUUIDVar(gameId) as session              =>
              for {
                isAccepted <- matches.isAccepted(gameId, session.userId)
                resp       <- if (isAccepted)
                                WebSocketBuilder[Task].build(matchToClient(gameId, session.userId), fromClient(gameId, session.userId)).resurrect
                              else
                                Task.succeed(Response[Task](status = Status.Forbidden))
              } yield resp
          }
          sessionMiddleware(routesWithSession)
        }

        private def fromClient(gameId: FUUID, userId: FUUID): Pipe[Task, WebSocketFrame, Unit] =
          _.evalMap {
            case WebSocketFrame.Text(text, _) => logger.info(s"Received frame $text")
            case WebSocketFrame.Close(_)      => logger.info("Client closed connection")
            case f                            => logger.info(s"Unknown frame type: $f")
          }.handleErrorWith {
            case EOFCommand => fs2.Stream.eval_(logger.debug(s"client $userId unexpectedly left the game $gameId anteroom"))
            case err        => fs2.Stream.eval_(logger.throwable("Unexpected error during processing input WS queue", err))
          }

        private def anteroomToClient(gameId: FUUID, userId: FUUID): fs2.Stream[Task, WebSocketFrame] =
          wsTopic
            .subscribe(16)
            .toFs2Stream
            .filter(m => verifyRecipient(m.recipient, gameId, userId))
            .collect {
              case m: WebSocketMessage.NewParticipant     => m.toFrame
              case m: WebSocketMessage.ParticipantRemoved => m.toFrame
              case m: WebSocketMessage.UserRoleChanged    => m.toFrame
              case m: WebSocketMessage.GameStarted        => m.toFrame
            }

        private def verifyRecipient(recipient: MessageRecipient, gameId: FUUID, userId: FUUID): Boolean =
          recipient match {
            case MessageRecipient.SingleGameParticipant(game, user)     => game === gameId && user === userId
            case MessageRecipient.MultipleGameParticipants(game, users) => game === gameId && users.contains(userId)
            case MessageRecipient.Broadcast                             => true
          }

        private def matchToClient(gameId: FUUID, userId: FUUID): fs2.Stream[Task, WebSocketFrame] =
          wsTopic.subscribe(16).toFs2Stream.filter(m => verifyRecipient(m.recipient, gameId, userId)).collect {
            case m: WebSocketMessage.ThreatStatusAssigned => m.toFrame
            case m: WebSocketMessage.NextPlayer           => m.toFrame
            case m: WebSocketMessage.NextRound            => m.toFrame
            case m: WebSocketMessage.GameFinished         => m.toFrame
            case m: WebSocketMessage.CardPlayed           => m.toFrame
            case m: WebSocketMessage.PlayerTakesTrick     => m.toFrame
          }
      }
    }

}
