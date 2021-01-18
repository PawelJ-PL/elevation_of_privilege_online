package com.github.pawelj_pl.eoponline.database

import com.github.pawelj_pl.eoponline.config.AppConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import zio.{Has, ZIO, ZLayer, blocking}
import zio.blocking.Blocking

object DataSourceLayers {

  val hikari: ZLayer[Blocking with Has[AppConfig.Database], Throwable, Has[DataSource]] = ZIO
    .accessM[Blocking with Has[AppConfig.Database]] { env =>
      blocking.effectBlocking {
        val dbConf = env.get[AppConfig.Database]
        val ds = new HikariDataSource()
        ds.setJdbcUrl(dbConf.url)
        ds.setUsername(dbConf.userName)
        ds.setPassword(dbConf.password)
        ds.setPoolName("HikariPool-EoP")
        ds.setConnectionTimeout(5000)
        ds.setMaximumPoolSize(dbConf.maxPoolSize)
        ds
      }
    }
    .toLayer

}
