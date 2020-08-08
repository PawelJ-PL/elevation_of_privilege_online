package com.github.paweljpl.eoponline.testdoubles

import com.github.pawelj_pl.eoponline.eventbus.InternalMessage
import fs2.Pipe
import fs2.concurrent.Topic
import zio.{Has, Ref, Task, ZLayer}

object FakeInternalMessagesTopic {

  def test(events: Ref[List[InternalMessage]]): ZLayer[Any, Nothing, Has[Topic[Task, InternalMessage]]] =
    ZLayer.succeed(
      new Topic[Task, InternalMessage] {

        override def publish: Pipe[Task, InternalMessage, Unit] = ???

        override def publish1(a: InternalMessage): Task[Unit] = events.update(prev => prev :+ a)

        override def subscribe(maxQueued: Int): fs2.Stream[Task, InternalMessage] = ???

        override def subscribeSize(maxQueued: Int): fs2.Stream[Task, (InternalMessage, Int)] = ???

        override def subscribers: fs2.Stream[Task, Int] = ???

      }
    )

}
