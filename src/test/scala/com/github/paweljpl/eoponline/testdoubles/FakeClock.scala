package com.github.paweljpl.eoponline.testdoubles

import java.time.{DateTimeException, OffsetDateTime}
import java.util.concurrent.TimeUnit

import com.github.paweljpl.eoponline.Constants
import zio.clock.Clock
import zio.duration.Duration
import zio.{IO, Ref, UIO, ZLayer}

object FakeClock extends Constants {

  val instance: ZLayer[Any, Nothing, Clock] = ZLayer.fromEffect(
    Ref
      .make(InitClockMillis)
      .map(ref =>
        new Clock.Service {

          override def currentTime(unit: TimeUnit): UIO[Long] = ref.getAndUpdate(_ + 1000).map(l => unit.convert(l, TimeUnit.MILLISECONDS))

          override def currentDateTime: IO[DateTimeException, OffsetDateTime] = ???

          override def nanoTime: UIO[Long] = ???

          override def sleep(duration: Duration): UIO[Unit] = ???

        }
      )
  )

}
