package com.github.pawelj_pl.eoponline.database

import com.github.pawelj_pl.eoponline.config.AppConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import zio.{Has, ZIO, ZLayer, blocking}
import zio.blocking.Blocking
import zio.config.ZConfig

object DataSourceLayers {

  val hikari: ZLayer[Blocking with ZConfig[AppConfig.Database], Throwable, Has[DataSource]] = ZIO
    .accessM[Blocking with ZConfig[AppConfig.Database]] { env =>
      blocking.effectBlocking {
        val dbConf = env.get[AppConfig.Database]
        val ds = new HikariDataSource()
        ds.setJdbcUrl(dbConf.url)
        ds.setUsername(dbConf.userName)
        ds.setPassword(dbConf.password)
        ds.setPoolName("HikariPool-EoP")
        ds.setMaximumPoolSize(dbConf.maxPoolSize)
        ds
      }
    }
    .toLayer

}
