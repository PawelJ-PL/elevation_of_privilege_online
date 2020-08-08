package com.github.paweljpl.eoponline.session

import cats.syntax.eq._
import cats.instances.string._
import com.github.pawelj_pl.eoponline.session.{Authentication, Session}
import com.github.pawelj_pl.eoponline.session.Authentication.Authentication
import com.github.paweljpl.eoponline.Constants
import com.github.paweljpl.eoponline.testdoubles.{FakeJwt, RandomMock}
import io.circe.literal.JsonStringContext
import org.http4s.{AuthedRoutes, Header, Headers, Request, Response, Status, Uri}
import org.http4s.dsl.Http4sDsl
import zio.{Task, ZLayer}
import zio.clock.Clock
import zio.logging.Logging
import zio.logging.slf4j.Slf4jLogger
import zio.test.Assertion.{equalTo, isNone, isSome}
import zio.test.{DefaultRunnableSpec, ZSpec, assert, suite, testM}

object AuthenticationSpec extends DefaultRunnableSpec with Constants {

  val logging: ZLayer[Any, Nothing, Logging] = Slf4jLogger.make((_, str) => str)

  val layers: ZLayer[Any, Nothing, Authentication] = Clock.live ++ FakeJwt.test ++ logging ++ RandomMock.test >>> Authentication.live

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("Middlewares suite")(
      authMiddlewareSuccess,
      authMiddlewareFailedWhenCookieNotProvided,
      authMiddlewareFailedWhenCookieWithDifferentNameProvided,
      authMiddlewareFailedWhenUnableToParse,
      sessionMiddlewareWhenSessionProvided,
      sessionMiddlewareGeneratesNewSession
    )

  object TestRoutes extends Http4sDsl[Task] {
    import zio.interop.catz._
    import org.http4s.implicits._

    val routes = AuthedRoutes
      .of[Session, Task] {
        case GET -> Root / "ok" as session =>
          Task.succeed(Response[Task](status = Status.Ok, headers = Headers.of(Header("X-Session-Id", session.userId.show))))
      }

  }

  private val authMiddlewareSuccess = testM("Authenticate user with success") {
    for {
      authMiddleware <- Authentication.middleware.provideCustomLayer(layers)
      token = json"""{"session": {"userId": "a5de7235-5560-42f7-b63f-5510b87f77f3", "createdAt":"2020-01-01T22:01:39.885Z"}}"""
      request = Request[Task](uri = Uri(path = "/ok")).addCookie("session", token.noSpaces)
      maybeResponse  <- authMiddleware(TestRoutes.routes)(request).value
    } yield assert(maybeResponse.flatMap(_.headers.find(_.name.value === "X-Session-Id").map(_.value)))(
      isSome(equalTo("a5de7235-5560-42f7-b63f-5510b87f77f3"))
    ) &&
      assert(maybeResponse.map(_.status))(isSome(equalTo(Status.Ok)))
  }

  private val authMiddlewareFailedWhenCookieNotProvided = testM("Authenticate user failed because no cookie") {
    for {
      authMiddleware <- Authentication.middleware.provideCustomLayer(layers)
      request = Request[Task](uri = Uri(path = "/ok"))
      maybeResponse  <- authMiddleware(TestRoutes.routes)(request).value
    } yield assert(maybeResponse.flatMap(_.headers.find(_.name.value === "X-Session-Id").map(_.value)))(isNone) &&
      assert(maybeResponse.map(_.status))(isSome(equalTo(Status.Unauthorized)))
  }

  private val authMiddlewareFailedWhenCookieWithDifferentNameProvided =
    testM("Authenticate user failed because of session cookie missing") {
      for {
        authMiddleware <- Authentication.middleware.provideCustomLayer(layers)
        token = json"""{"session": {"userId": "a5de7235-5560-42f7-b63f-5510b87f77f3", "createdAt":"2020-01-01T22:01:39.885Z"}}"""
        request = Request[Task](uri = Uri(path = "/ok")).addCookie("foo", token.noSpaces)
        maybeResponse  <- authMiddleware(TestRoutes.routes)(request).value
      } yield assert(maybeResponse.flatMap(_.headers.find(_.name.value === "X-Session-Id").map(_.value)))(isNone) &&
        assert(maybeResponse.map(_.status))(isSome(equalTo(Status.Unauthorized)))
    }

  private val authMiddlewareFailedWhenUnableToParse = testM("Authenticate user failed because of parser returns error") {
    for {
      authMiddleware <- Authentication.middleware.provideCustomLayer(layers)
      token = json"""{"session": {"userIdx": "a5de7235-5560-42f7-b63f-5510b87f77f3", "createdAt":"2020-01-01T22:01:39.885Z"}}"""
      request = Request[Task](uri = Uri(path = "/ok")).addCookie("session", token.noSpaces)
      maybeResponse  <- authMiddleware(TestRoutes.routes)(request).value
    } yield assert(maybeResponse.flatMap(_.headers.find(_.name.value === "X-Session-Id").map(_.value)))(isNone) &&
      assert(maybeResponse.map(_.status))(isSome(equalTo(Status.Unauthorized)))
  }

  private val sessionMiddlewareWhenSessionProvided = testM("Session middleware use provided session") {
    for {
      sessionMiddleware <- Authentication.sessionMiddleware.provideCustomLayer(layers)
      token = json"""{"session": {"userId": "a5de7235-5560-42f7-b63f-5510b87f77f3", "createdAt":"2020-01-01T22:01:39.885Z"}}"""
      request = Request[Task](uri = Uri(path = "/ok")).addCookie("session", token.noSpaces)
      maybeResponse     <- sessionMiddleware(TestRoutes.routes)(request).value
    } yield assert(maybeResponse.flatMap(_.headers.find(_.name.value === "X-Session-Id").map(_.value)))(
      isSome(equalTo("a5de7235-5560-42f7-b63f-5510b87f77f3"))
    ) &&
      assert(maybeResponse.map(_.status))(isSome(equalTo(Status.Ok)))
  }

  private val sessionMiddlewareGeneratesNewSession = testM("Session middleware generates a new session") {
    for {
      sessionMiddleware <- Authentication.sessionMiddleware.provideCustomLayer(layers)
      request = Request[Task](uri = Uri(path = "/ok"))
      maybeResponse     <- sessionMiddleware(TestRoutes.routes)(request).value
    } yield assert(maybeResponse.flatMap(_.headers.find(_.name.value === "X-Session-Id").map(_.value)))(
      isSome(equalTo(FirstRandomFuuid.show))
    ) &&
      assert(maybeResponse.map(_.status))(isSome(equalTo(Status.Ok)))
  }

}
