package com.github.pawelj_pl.eoponline.eventbus

import fs2.concurrent.Topic
import zio.{Has, Task, ZLayer}
import zio.interop.catz._

object TopicLayer {

  val live: ZLayer[Any, Throwable, Has[Topic[Task, InternalMessage]]] =
    ZLayer.fromEffect(Topic[Task, InternalMessage](InternalMessage.TopicStarted))

}
