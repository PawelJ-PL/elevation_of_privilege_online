package com.github.pawelj_pl.eoponline.session

import cats.data.{Kleisli, OptionT}
import cats.instances.string._
import cats.syntax.eq._
import com.github.pawelj_pl.eoponline.session.Jwt.Jwt
import com.github.pawelj_pl.eoponline.utils.RandomUtils
import com.github.pawelj_pl.eoponline.utils.RandomUtils.RandomUtils
import org.http4s.{ContextRequest, Request, Response, Status}
import org.http4s.server.{AuthMiddleware, Middleware}
import zio.clock.Clock
import zio.interop.catz._
import zio.logging.{Logger, Logging}
import zio.{Has, Task, UIO, ZIO, ZLayer}

object Authentication {

  type Authentication = Has[Authentication.Service]

  type SessionMiddleware = Middleware[OptionT[Task, *], ContextRequest[Task, Session], Response[Task], Request[Task], Response[Task]]

  type SessionRoutes = Kleisli[OptionT[Task, *], ContextRequest[Task, Session], Response[Task]]

  trait Service {

    def middleware: ZIO[Any, Nothing, AuthMiddleware[Task, Session]]

    def sessionMiddleware: UIO[SessionMiddleware]

  }

  def middleware: ZIO[Authentication, Nothing, AuthMiddleware[Task, Session]] = ZIO.accessM[Authentication](_.get.middleware)

  def sessionMiddleware: ZIO[Authentication, Nothing, SessionMiddleware] = ZIO.accessM[Authentication](_.get.sessionMiddleware)

  val live: ZLayer[Clock with Jwt with Logging with RandomUtils, Nothing, Has[Service]] =
    ZLayer.fromServices[Clock.Service, Jwt.Service, Logger[String], RandomUtils.Service, Authentication.Service] {
      (clock, jwt, logger, randomUtils) =>
        new Service {
          private final val CookieName = "session"

          private def reqToSession(req: Request[Task]): OptionT[Task, Session] =
            for {
              cookie  <- OptionT.fromOption[Task](req.cookies.find(_.name === CookieName))
              session <- OptionT[Task, Session](
                           jwt
                             .decode(cookie.content)
                             .map(Option(_))
                             .catchAll(err => logger.throwable("Unable to parse JWT token", err).as(Option.empty))
                         )
            } yield session

          override def middleware: ZIO[Any, Nothing, AuthMiddleware[Task, Session]] = {
            val authService = Kleisli[OptionT[Task, *], Request[Task], Session](reqToSession)
            ZIO.succeed(AuthMiddleware(authService))
          }

          private def extractOrCreateSession(req: Request[Task]): Task[Session] = reqToSession(req).getOrElseF(createSession)

          private def createSession: Task[Session] =
            for {
              now    <- clock.instant
              userId <- randomUtils.randomFuuid
            } yield Session(userId, now)

          def responseWithCookie(resp: Response[Task], session: Session): Task[Response[Task]] =
            jwt.encode(session).map(token => resp.addCookie(CookieName, token))

          override def sessionMiddleware: UIO[SessionMiddleware] = {
            val middleware: SessionMiddleware = { service =>
              Kleisli { (req: Request[Task]) =>
                val sessionService = Kleisli[Task, Request[Task], Session](extractOrCreateSession)
                val resp =
                  sessionService(req)
                    .flatMap(session =>
                      service(ContextRequest(session, req))
                        .semiflatMap(resp => responseWithCookie(resp, session))
                        .getOrElse(Response[Task](Status.NotFound))
                    )
                OptionT.liftF(resp)
              }
            }
            ZIO.succeed(middleware)
          }

        }
    }

}
