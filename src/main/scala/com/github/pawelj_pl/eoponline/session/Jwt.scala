package com.github.pawelj_pl.eoponline.session

import java.util.Base64

import com.github.pawelj_pl.eoponline.config.AppConfig
import tsec.jws.mac.JWTMac
import tsec.jwt.JWTClaims
import tsec.mac.jca.HMACSHA256
import zio.clock.Clock
import zio.config.ZConfig
import zio.random.Random
import zio.{Has, Task, ZIO, ZLayer}
import zio.interop.catz._

object Jwt {

  type Jwt = Has[Jwt.Service]

  trait Service {

    def encode(session: Session): Task[String]

    def decode(token: String): Task[Session]

  }

  def encode(session: Session): ZIO[Jwt, Throwable, String] = ZIO.accessM[Jwt](_.get.encode(session))

  def decode(token: String): ZIO[Jwt, Throwable, Session] = ZIO.accessM[Jwt](_.get.decode(token))

  val live: ZLayer[Clock with Random with ZConfig[AppConfig.Auth], Nothing, Has[Service]] =
    ZLayer.fromServices[Clock.Service, Random.Service, AppConfig.Auth, Jwt.Service] { (clock, random, authConfig) =>
      new Service {
        private final val SessionFieldKey = "session"

        override def encode(session: Session): Task[String] =
          for {
            now    <- clock.instant
            jwtId  <- random.nextBytes(32).map(bytes => Base64.getEncoder.encodeToString(bytes.toArray))
            claims <-
              JWTClaims(issuer = Some("EoP Online"), subject = Some("EoP Online Session"), issuedAt = Some(now), jwtId = Some(jwtId))
                .withCustomFieldF[Task, Session](SessionFieldKey, session)
            key    <- HMACSHA256.buildKey[Task](authConfig.secretKey.getBytes)
            token  <- JWTMac.buildToString[Task, HMACSHA256](claims, key)
          } yield token

        override def decode(token: String): Task[Session] =
          for {
            key     <- HMACSHA256.buildKey[Task](authConfig.secretKey.getBytes)
            parsed  <- JWTMac.verifyAndParse[Task, HMACSHA256](token, key)
            session <- parsed.body.getCustomF[Task, Session](SessionFieldKey)
          } yield session
      }
    }

}
