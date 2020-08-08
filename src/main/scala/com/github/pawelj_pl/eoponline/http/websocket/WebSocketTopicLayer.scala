package com.github.pawelj_pl.eoponline.http.websocket

import fs2.concurrent.Topic
import zio.{Has, Task, ZLayer}
import zio.interop.catz._

object WebSocketTopicLayer {

  val live: ZLayer[Any, Throwable, Has[Topic[Task, WebSocketMessage[_]]]] =
    ZLayer.fromEffect(Topic.apply[Task, WebSocketMessage[_]](WebSocketMessage.TopicStarted))

}
