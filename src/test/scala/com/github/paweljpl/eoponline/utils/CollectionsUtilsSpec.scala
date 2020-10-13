package com.github.paweljpl.eoponline.utils

import com.github.pawelj_pl.eoponline.utils.CollectionsUtils.syntax._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CollectionsUtilsSpec extends AnyWordSpec with Matchers {

  val input: Seq[Int] = (1 to 15).toList

  "collection utils" should {
    "distribute elements" when {
      "there is a single consumer" in {
        val consumers = 1
        val result = input.distribute(consumers)
        result shouldBe Vector((1 to 15).toList)
      }

      "there is fairly divisible number of consumers" in {
        val consumers = 3
        val result = input.distribute(consumers)
        result shouldBe Vector(
          List(1, 4, 7, 10, 13),
          List(2, 5, 8, 11, 14),
          List(3, 6, 9, 12, 15)
        )
      }

      "number of consumer is not fairly divisible" in {
        val consumers = 6
        val result = input.distribute(consumers)
        result shouldBe Vector(
          List(1, 7, 13),
          List(2, 8, 14),
          List(3, 9, 15),
          List(4, 10),
          List(5, 11),
          List(6, 12)
        )
      }

      "number of consumers is equal to collection size" in {
        val consumers = 15
        val result = input.distribute(consumers)
        result shouldBe Vector(
          List(1),
          List(2),
          List(3),
          List(4),
          List(5),
          List(6),
          List(7),
          List(8),
          List(9),
          List(10),
          List(11),
          List(12),
          List(13),
          List(14),
          List(15)
        )
      }

      "number of consumers is grater than collection size" in {
        val consumers = 18
        val result = input.distribute(consumers)
        result shouldBe Vector(
          List(1),
          List(2),
          List(3),
          List(4),
          List(5),
          List(6),
          List(7),
          List(8),
          List(9),
          List(10),
          List(11),
          List(12),
          List(13),
          List(14),
          List(15),
          List(),
          List(),
          List()
        )
      }
    }
  }
}
