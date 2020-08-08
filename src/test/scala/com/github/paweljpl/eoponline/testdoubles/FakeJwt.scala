package com.github.paweljpl.eoponline.testdoubles

import com.github.pawelj_pl.eoponline.session.Jwt.Jwt
import com.github.pawelj_pl.eoponline.session.{Jwt, Session}
import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.deriveCodec
import io.circe.parser.parse
import zio.{Task, ULayer, ZLayer}

object FakeJwt {

  private case class Token(session: Session)

  private object Token {

    implicit val codec: Codec[Token] = deriveCodec

  }

  val test: ULayer[Jwt] = ZLayer.succeed(new Jwt.Service {

    override def encode(session: Session): Task[String] = Task.succeed(Encoder[Token].apply(Token(session)).noSpaces)

    override def decode(token: String): Task[Session] =
      Task.fromEither(parse(token).flatMap(json => Decoder[Token].decodeJson(json)).map(_.session))

  })

}
