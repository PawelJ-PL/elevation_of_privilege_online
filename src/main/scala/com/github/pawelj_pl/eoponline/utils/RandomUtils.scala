package com.github.pawelj_pl.eoponline.utils

import io.chrisdavenport.fuuid.FUUID
import zio.{Has, Task, UIO, ZIO, ZLayer}
import zio.interop.catz._
import zio.random.Random

import scala.collection.compat.BuildFrom

object RandomUtils {

  type RandomUtils = Has[RandomUtils.Service]

  trait Service {

    def randomFuuid: ZIO[Any, Nothing, FUUID]

    def shuffle[A, Collection[+Element] <: Iterable[Element]](
      collection: Collection[A]
    )(
      implicit bf: BuildFrom[Collection[A], A, Collection[A]]
    ): UIO[Collection[A]]

  }

  val live: ZLayer[Random, Nothing, RandomUtils] = ZLayer.fromService[Random.Service, RandomUtils.Service] { rand =>
    new Service {

      override def randomFuuid: ZIO[Any, Nothing, FUUID] = FUUID.randomFUUID[Task].orDie

      override def shuffle[A, Collection[+Element] <: Iterable[Element]](
        collection: Collection[A]
      )(
        implicit bf: BuildFrom[Collection[A], A, Collection[A]]
      ): UIO[Collection[A]] = rand.shuffle(collection)
    }
  }

}
