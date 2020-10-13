package com.github.pawelj_pl.eoponline.`match`.repository

import cats.instances.int._
import cats.syntax.eq._
import com.github.pawelj_pl.eoponline.`match`.{Card, Suit, Value}
import io.circe.{Decoder, parser}
import zio.{Has, Managed, ZIO, ZLayer}

import scala.io.Source

object CardsRepository {

  type CardsRepository = Has[CardsRepository.Service]

  trait Service {

    def get(value: Value, suit: Suit): ZIO[Any, Exception, Option[Card]]

    def get(cardNumber: Int): ZIO[Any, Exception, Option[Card]]

  }

  val fromJsonFile: ZLayer[Any, Throwable, CardsRepository] = ZLayer.fromEffect(
    Managed
      .make[Any, Any, Throwable, Source](ZIO(Source.fromResource("cards.json")))(resource => ZIO(resource.close()).orDie)
      .map(_.mkString)
      .use(jsonString => ZIO.fromEither(parser.parse(jsonString).flatMap(Decoder[List[Card]].decodeJson(_)).map(_.toSet)))
      .map(cards =>
        new Service {

          override def get(value: Value, suit: Suit): ZIO[Any, Exception, Option[Card]] =
            ZIO.succeed(cards.find(card => card.value === value && card.suit === suit))

          override def get(cardNumber: Int): ZIO[Any, Exception, Option[Card]] = ZIO.succeed(cards.find(_.cardNumber === cardNumber))

        }
      )
  )

}
