package com.github.pawelj_pl.eoponline.utils

import zio.ZIO

object ZioUtils {

  object implicits {

    implicit class ZioGenericOps[R, E, A](input: ZIO[R, E, A]) {

      def flatTap[R1 <: R, E1 >: E, B](operation: A => ZIO[R1, E1, B]): ZIO[R1, E1, A] = input.flatMap(k => operation(k).map(_ => k))

    }

  }

}
