package com.github.pawelj_pl.eoponline.eventbus.broker

import com.gh.dobrynya.zio.jms.{BlockingConnection, DestinationFactory, JmsConsumer, JmsProducer, onlyText, textMessageEncoder}
import fs2.concurrent.Topic
import io.circe
import io.circe.{Decoder, Encoder, parser}
import io.circe.syntax._
import izumi.reflect.Tag
import zio.stream.ZStream
import zio.{Has, Task, ZLayer}
import zio.interop.catz._
import zio.logging.{Logger, Logging}
import zio.stream.interop.fs2z._

object MessageTopic {

  type MessageTopic[A] = Has[MessageTopic.Service[A]]

  trait Service[A] {

    def publish(message: A): Task[Unit]

    def subscribe(maxQueued: Int): ZStream[Any, Throwable, A]

  }

  def inMemory[A: Tag](initialMessage: A): ZLayer[Any, Throwable, MessageTopic[A]] =
    ZLayer.fromEffect(
      Topic[Task, A](initialMessage).map(topic =>
        new Service[A] {

          override def publish(message: A): Task[Unit] = topic.publish1(message)

          override def subscribe(maxQueued: Int): ZStream[Any, Throwable, A] =
            topic.subscribe(maxQueued).toZStream(16)

        }
      )
    )

  def jms[A: Tag: Encoder: Decoder](
    destination: DestinationFactory
  ): ZLayer[Has[BlockingConnection] with Logging, Throwable, Has[Service[A]]] =
    ZLayer.fromServices[BlockingConnection, Logger[String], MessageTopic.Service[A]] { (connection, logger) =>
      new Service[A] {
        override def publish(message: A): Task[Unit] =
          ZStream.succeed(message.asJson.noSpaces).run(JmsProducer.sink(destination, textMessageEncoder)).provide(connection)

        override def subscribe(maxQueued: Int): ZStream[Any, Throwable, A] =
          JmsConsumer
            .consume(destination)
            .collect(onlyText)
            .map(decodeJsonMessage)
            .flatMap {
              case r @ Left(value)  => ZStream.fromEffect(logger.warn(s"Error during parsing message: $value").as(r))
              case r @ Right(value) => ZStream.fromEffect(logger.trace(s"Decoded message $value")).as(r)
            }
            .collectRight
            .provide(connection)

        private def decodeJsonMessage(message: String): Either[circe.Error, A] =
          parser.parse(message).flatMap(json => Decoder[A].decodeJson(json))
      }
    }

}
