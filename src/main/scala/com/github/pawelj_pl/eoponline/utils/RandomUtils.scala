package com.github.pawelj_pl.eoponline.utils

import io.chrisdavenport.fuuid.FUUID
import zio.{Has, Task, ULayer, ZIO, ZLayer}
import zio.interop.catz._

object RandomUtils {

  type RandomUtils = Has[RandomUtils.Service]

  trait Service {

    def randomFuuid: ZIO[Any, Nothing, FUUID]

  }

  val live: ULayer[RandomUtils] = ZLayer.succeed(new Service {

    override def randomFuuid: ZIO[Any, Nothing, FUUID] = FUUID.randomFUUID[Task].orDie

  })

}
