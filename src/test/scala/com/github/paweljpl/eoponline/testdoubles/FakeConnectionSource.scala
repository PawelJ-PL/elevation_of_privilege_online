package com.github.paweljpl.eoponline.testdoubles

import java.sql.Connection

import io.github.gaelrenoux.tranzactio.{ConnectionSource, DbException, ErrorStrategiesRef}
import zio.{ULayer, ZIO, ZLayer}

object FakeConnectionSource {

  val test: ULayer[ConnectionSource] = ZLayer.succeed(new ConnectionSource.Service {

    override def openConnection(implicit errorStrategies: ErrorStrategiesRef): ZIO[Any, DbException, Connection] =
      ZIO.succeed(null.asInstanceOf[Connection])

    override def setAutoCommit(
      c: Connection,
      autoCommit: Boolean
    )(
      implicit errorStrategies: ErrorStrategiesRef
    ): ZIO[Any, DbException, Unit] = ZIO.unit

    override def commitConnection(c: Connection)(implicit errorStrategies: ErrorStrategiesRef): ZIO[Any, DbException, Unit] = ZIO.unit

    override def rollbackConnection(c: Connection)(implicit errorStrategies: ErrorStrategiesRef): ZIO[Any, DbException, Unit] = ZIO.unit

    override def closeConnection(c: Connection)(implicit errorStrategies: ErrorStrategiesRef): ZIO[Any, DbException, Unit] = ZIO.unit

  })

}
