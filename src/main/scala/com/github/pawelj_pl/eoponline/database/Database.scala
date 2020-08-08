package com.github.pawelj_pl.eoponline.database

import java.sql.DriverManager

import com.github.pawelj_pl.eoponline.config.AppConfig
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import zio.config.ZConfig
import zio.{Has, Task, ZIO, ZLayer, ZManaged}

object Database {

  type Database = Has[Database.Service]

  trait Service {

    def migrate: Task[Unit]

  }

  def migrate: ZIO[Database, Throwable, Unit] = ZIO.accessM[Database](_.get.migrate)

  val live: ZLayer[ZConfig[AppConfig.Database], Nothing, Database] = ZLayer.fromService[AppConfig.Database, Database.Service](dbConfig =>
    new Service {

      private final val LiquibaseChangelogMaster = "db/changelog/changelog-master.yml"

      override def migrate: Task[Unit] =
        (for {
          connection <- ZManaged.fromAutoCloseable(Task(DriverManager.getConnection(dbConfig.url, dbConfig.userName, dbConfig.password)))
          database   <- ZManaged.make(
                          Task(DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection)))
                        )(connection => Task(connection.close()).orDie)
          liquibase  <- ZManaged.fromEffect(
                          Task(new Liquibase(LiquibaseChangelogMaster, new ClassLoaderResourceAccessor(getClass.getClassLoader), database))
                        )
        } yield liquibase).use(liquibase => Task(liquibase.update("main")))

    }
  )

}
