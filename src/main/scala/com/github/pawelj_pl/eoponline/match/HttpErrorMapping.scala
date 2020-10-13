package com.github.pawelj_pl.eoponline.`match`

import com.github.pawelj_pl.eoponline.ResponseData
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.{Response, Status}
import zio.{Task, UIO, ZIO}

private[`match`] object HttpErrorMapping {

  def mapMatchError(error: MatchError): UIO[Response[Task]] =
    error match {
      case err: MatchInfoError => mapMatchInfoErrors(err)
    }

  private def mapMatchInfoErrors(error: MatchInfoError): UIO[Response[Task]] =
    error match {
      case GameNotFound(_)     => ZIO.succeed(Response[Task](status = Status.NotFound).withEntity(ResponseData("Match not found")))
      case NotGameMember(_, _) => ZIO.succeed(Response[Task](status = Status.NotFound).withEntity(ResponseData("Match not found")))
    }

}
