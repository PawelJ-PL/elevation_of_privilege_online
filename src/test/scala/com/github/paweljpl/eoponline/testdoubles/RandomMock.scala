package com.github.paweljpl.eoponline.testdoubles

import com.github.pawelj_pl.eoponline.utils.RandomUtils
import com.github.pawelj_pl.eoponline.utils.RandomUtils.RandomUtils
import com.github.paweljpl.eoponline.Constants
import io.chrisdavenport.fuuid.FUUID
import zio.{Ref, ZIO, ZLayer}

object RandomMock extends Constants {

  val test: ZLayer[Any, Nothing, RandomUtils] = ZLayer.fromEffect(
    Ref
      .make[List[FUUID]](List(
        FirstRandomFuuid
      ))
      .map(ref =>
        new RandomUtils.Service {

          override def randomFuuid: ZIO[Any, Nothing, FUUID] =
            ref
              .get
              .flatMap {
                case ::(head, next) =>
                  ref.set(next).map(_ => head)
                case Nil            => ZIO.fail(new RuntimeException("No more random FUUIDs"))
              }
              .orDie

        }
      )
  )

}
