package com.github.paweljpl.eoponline.testdoubles

import cats.instances.int._
import cats.syntax.eq._
import com.github.pawelj_pl.eoponline.`match`.{Card, Suit, Value}
import com.github.pawelj_pl.eoponline.`match`.repository.CardsRepository
import com.github.pawelj_pl.eoponline.`match`.repository.CardsRepository.CardsRepository
import zio.{ULayer, ZIO, ZLayer}

object CardsRepoStub {

  val instance: ULayer[CardsRepository] = ZLayer.succeed(new CardsRepository.Service {

    val cards: Set[Card] = Set(
      Card(cardNumber = 1, value = Value.Two, suit = Suit.Spoofing, "TextFoo"),
      Card(cardNumber = 2, value = Value.Three, suit = Suit.Spoofing, "TextBar"),
      Card(cardNumber = 3, value = Value.Four, suit = Suit.Spoofing, "TextBaz"),
      Card(cardNumber = 14, value = Value.Three, suit = Suit.Tampering, "TextQux"),
      Card(cardNumber = 25, value = Value.Ace, suit = Suit.Tampering, "TextQuux"),
      Card(cardNumber = 32, value = Value.Eight, suit = Suit.Repudiation, "TestQuuz")
    )

    override def get(value: Value, suit: Suit): ZIO[Any, Exception, Option[Card]] =
      ZIO.succeed(cards.find(card => card.value === value && card.suit === suit))

    override def get(cardNumber: Int): ZIO[Any, Exception, Option[Card]] = ZIO.succeed(cards.find(_.cardNumber === cardNumber))

  })

}
