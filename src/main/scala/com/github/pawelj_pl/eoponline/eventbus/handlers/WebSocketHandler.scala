package com.github.pawelj_pl.eoponline.eventbus.handlers

import cats.instances.int._
import cats.syntax.show._
import com.github.pawelj_pl.eoponline.`match`.CardLocation
import com.github.pawelj_pl.eoponline.`match`.dto.ExtendedDeckElementDto
import com.github.pawelj_pl.eoponline.`match`.repository.CardsRepository
import com.github.pawelj_pl.eoponline.`match`.repository.CardsRepository.CardsRepository
import com.github.pawelj_pl.eoponline.eventbus.InternalMessage
import com.github.pawelj_pl.eoponline.eventbus.broker.MessageTopic
import com.github.pawelj_pl.eoponline.eventbus.broker.MessageTopic.MessageTopic
import com.github.pawelj_pl.eoponline.game.repository.GamesRepository
import com.github.pawelj_pl.eoponline.game.repository.GamesRepository.GamesRepository
import com.github.pawelj_pl.eoponline.http.websocket.{MessageRecipient, WebSocketMessage}
import fs2.concurrent.Topic
import io.chrisdavenport.fuuid.FUUID
import io.github.gaelrenoux.tranzactio.doobie.Database
import zio.{Has, Task, UIO, URIO, ZIO, ZLayer}
import zio.interop.catz._
import zio.logging.{Logger, Logging}

object WebSocketHandler {

  type WebSocketHandler = Has[WebSocketHandler.Service]

  trait Service {

    def handle: UIO[Unit]

  }

  def handle: ZIO[WebSocketHandler, Nothing, Unit] = ZIO.accessM[WebSocketHandler](_.get.handle)

  val live: ZLayer[Has[Topic[Task, InternalMessage]] with Logging with MessageTopic[WebSocketMessage[_]] with Has[
    Database.Service
  ] with GamesRepository with CardsRepository, Nothing, WebSocketHandler] =
    ZLayer.fromServices[Topic[Task, InternalMessage], Logger[String], MessageTopic.Service[
      WebSocketMessage[_]
    ], Database.Service, GamesRepository.Service, CardsRepository.Service, WebSocketHandler.Service] {
      (messageTopic, logger, wsTopic, db, gamesRepo, cardsRepo) =>
        new Service {

          override def handle: UIO[Unit] =
            messageTopic
              .subscribe(10)
              .evalMap(handleMessage)
              .compile
              .drain
              .resurrect
              .catchAll(err => logger.throwable("Web socket handler died", err))

          private def handleMessage(message: InternalMessage): ZIO[Any, Nothing, Unit] = {
            message match {
              case m @ InternalMessage.ParticipantJoined(gameId, _, _)          =>
                allAccepted(gameId).flatMap(recipients => wsTopic.publish(WebSocketMessage.NewParticipant(recipients, m)))
              case m @ InternalMessage.ParticipantKicked(gameId, removedUserId) =>
                allAcceptedWith(gameId, removedUserId).flatMap(recipients =>
                  wsTopic.publish(WebSocketMessage.ParticipantRemoved(recipients, m))
                )
              case m @ InternalMessage.RoleAssigned(gameId, _, _)               =>
                allAccepted(gameId).flatMap(recipients => wsTopic.publish(WebSocketMessage.UserRoleChanged(recipients, m)))
              case m @ InternalMessage.GameStarted(gameId: FUUID)               =>
                allAccepted(gameId).flatMap(recipients => wsTopic.publish(WebSocketMessage.GameStarted(recipients, m)))
              case m @ InternalMessage.ThreatLinkedStatusChanged(gameId, _, _, _)  =>
                allAccepted(gameId).flatMap(recipients => wsTopic.publish(WebSocketMessage.ThreatStatusAssigned(recipients, m)))
              case m @ InternalMessage.NextPlayer(gameId, _)                    =>
                allAccepted(gameId).flatMap(recipients => wsTopic.publish(WebSocketMessage.NextPlayer(recipients, m)))
              case m @ InternalMessage.NextRound(gameId, _)                     =>
                allAccepted(gameId).flatMap(recipients => wsTopic.publish(WebSocketMessage.NextRound(recipients, m)))
              case m @ InternalMessage.GameFinished(gameId)                     =>
                allAccepted(gameId).flatMap(recipients => wsTopic.publish(WebSocketMessage.GameFinished(recipients, m)))
              case m @ InternalMessage.CardPlayed(gameId, _, _)                 =>
                for {
                  recipients  <- allAccepted(gameId)
                  extendedDto <- playedCardToExtendedDto(m)
                  _           <- wsTopic.publish(WebSocketMessage.CardPlayed(recipients, extendedDto))
                } yield ()
              case m @ InternalMessage.PlayerTakesTrick(gameId, _)              =>
                allAccepted(gameId).flatMap(recipients => wsTopic.publish(WebSocketMessage.PlayerTakesTrick(recipients, m)))
              case m                                                            => logger.trace(s"Message $m ignored")
            }
          }.resurrect.catchAll(error => logger.throwable("Unable to process message", error))

          private def acceptedParticipants(gameId: FUUID): URIO[Any, List[FUUID]] =
            db.transactionOrDie(gamesRepo.getAcceptedParticipants(gameId)).orDie.map(_.map(_.id))

          private def allAccepted(gameId: FUUID): ZIO[Any, Nothing, MessageRecipient.MultipleGameParticipants] =
            acceptedParticipants(gameId).map(ids => MessageRecipient.MultipleGameParticipants(gameId, ids))

          private def allAcceptedWith(gameId: FUUID, userId: FUUID): ZIO[Any, Nothing, MessageRecipient.MultipleGameParticipants] =
            allAccepted(gameId).map(recipients => recipients.copy(userIds = recipients.userIds.appended(userId)))

          private def playedCardToExtendedDto(event: InternalMessage.CardPlayed): ZIO[Any, Throwable, ExtendedDeckElementDto] =
            cardsRepo
              .get(event.cardNumber)
              .someOrFail(new RuntimeException(show"Card ${event.cardNumber} not found"))
              .map(card => ExtendedDeckElementDto(event.gameId, event.player, card, CardLocation.Table, None))
        }
    }

}
