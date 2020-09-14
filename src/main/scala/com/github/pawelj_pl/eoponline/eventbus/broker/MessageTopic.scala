package com.github.pawelj_pl.eoponline.eventbus.broker

import fs2.concurrent.Topic
import izumi.reflect.Tag
import zio.stream.ZStream
import zio.{Has, Task, ZLayer}
import zio.interop.catz._
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

}
