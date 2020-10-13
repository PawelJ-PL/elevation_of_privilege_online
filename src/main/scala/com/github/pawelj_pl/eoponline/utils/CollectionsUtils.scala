package com.github.pawelj_pl.eoponline.utils

object CollectionsUtils {

  object syntax {

    implicit class SeqOps[A](seq: Seq[A]) {

      def distribute(parts: Int): Seq[Seq[A]] = (0 until parts).map(i => seq.drop(i).sliding(1, parts).flatten.toSeq)

    }

  }

}
