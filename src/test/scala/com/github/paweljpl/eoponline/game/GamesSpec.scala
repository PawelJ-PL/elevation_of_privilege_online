package com.github.paweljpl.eoponline.game

import com.github.pawelj_pl.eoponline.eventbus.InternalMessage
import com.github.pawelj_pl.eoponline.game.{
  GameAlreadyStarted,
  GameNotFound,
  Games,
  KickSelfForbidden,
  ParticipantAlreadyJoined,
  ParticipantIsNotAMember,
  ParticipantNotAccepted,
  Player,
  PlayerRole,
  UserIsNotGameOwner
}
import com.github.pawelj_pl.eoponline.game.Games.Games
import com.github.pawelj_pl.eoponline.game.dto.NewGameDto
import com.github.paweljpl.eoponline.Constants
import com.github.paweljpl.eoponline.testdoubles.{FakeConnectionSource, FakeGameRepo, FakeInternalMessagesTopic, RandomMock}
import com.github.paweljpl.eoponline.testdoubles.FakeGameRepo.GamesRepoState
import zio.logging.Logging
import zio.logging.slf4j.Slf4jLogger
import zio.{Ref, ZIO, ZLayer}
import zio.test.Assertion.{equalTo, hasSameElements, isEmpty, isLeft, isUnit}
import zio.test.environment.TestEnvironment
import zio.test.{DefaultRunnableSpec, ZSpec, assert, suite, testM}
import io.github.gaelrenoux.tranzactio.doobie.{Database => DoobieDb}
import zio.blocking.Blocking

object GamesSpec extends DefaultRunnableSpec with Constants {

  val logging: ZLayer[Any, Nothing, Logging] = Slf4jLogger.make((_, str) => str)

  val db: ZLayer[Any with Blocking, Nothing, DoobieDb.Database] =
    FakeConnectionSource.test ++ Blocking.any >>> DoobieDb.fromConnectionSource

  private def createLayer(gamesRepoState: Ref[GamesRepoState], sentEvents: Ref[List[InternalMessage]]) =
    FakeGameRepo.withState(gamesRepoState) ++ logging ++ FakeInternalMessagesTopic.test(sentEvents) ++ db ++ RandomMock.test >>> Games.live

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("Game setup suite")(
      createGame,
      readGame,
      readMissingGame,
      readGameWhenNotMember,
      readGameWhenNotAccepted,
      joinGame,
      joinGameWhenGameNotFound,
      joinGameWhenAlreadyStarted,
      joinGameWhenAlreadyJoined,
      getParticipants,
      getParticipantsWhenNotAMember,
      getParticipantsWhenNotAccepted,
      kickParticipant,
      kickParticipantWhenGameStarted,
      kickParticipantWhenNotAnOwner,
      kickParticipantSelf,
      assignRole,
      assignRoleWhenGameStarted,
      assignRoleWhenGameNotFound,
      assignRoleWhenNotAnOwner
    )

  private val newGameDto = NewGameDto(Some(ExampleGameDescription), ExampleNickName)

  private val createGame =
    testM("Create game with success")(
      for {
        repoState     <- Ref.make[GamesRepoState](GamesRepoState())
        sentEvents    <- Ref.make[List[InternalMessage]](List.empty)
        game          <- ZIO
                           .accessM[Games](_.get.create(newGameDto, ExampleUserId))
                           .provideCustomLayer(createLayer(repoState, sentEvents))
        updatedRep    <- repoState.get
        updatedEvents <- sentEvents.get
      } yield {
        val expectedGame = ExampleNotStartedGame.copy(id = FirstRandomFuuid)
        val expectedEvent = InternalMessage.GameCreated(expectedGame)
        assert(game)(equalTo(expectedGame)) &&
        assert(updatedRep)(equalTo(GamesRepoState(Set(expectedGame), Map(FirstRandomFuuid -> Set(ExamplePlayer))))) &&
        assert(updatedEvents)(equalTo(List(expectedEvent)))
      }
    )

  private val readGame = testM("Read game info with success") {
    val initialRepoState = GamesRepoState(games = Set(ExampleNotStartedGame), players = Map(ExampleNotStartedGame.id -> Set(ExamplePlayer)))
    for {
      repoState     <- Ref.make[GamesRepoState](initialRepoState)
      sentEvents    <- Ref.make[List[InternalMessage]](List.empty)
      game          <- ZIO
                         .accessM[Games](_.get.getInfoAs(ExampleGameId, ExampleUserId))
                         .provideCustomLayer(createLayer(repoState, sentEvents))
      updatedRep    <- repoState.get
      updatedEvents <- sentEvents.get
    } yield assert(game)(equalTo(ExampleNotStartedGame)) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val readMissingGame = testM("Read game info with Game not found error") {
    val initialRepoState = GamesRepoState(games = Set.empty, players = Map(ExampleNotStartedGame.id -> Set(ExamplePlayer)))
    for {
      repoState     <- Ref.make[GamesRepoState](initialRepoState)
      sentEvents    <- Ref.make[List[InternalMessage]](List.empty)
      result        <- ZIO
                         .accessM[Games](_.get.getInfoAs(ExampleGameId, ExampleUserId))
                         .provideCustomLayer(createLayer(repoState, sentEvents))
                         .either
      updatedRep    <- repoState.get
      updatedEvents <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(GameNotFound(ExampleGameId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val readGameWhenNotMember = testM("Read game info with user is not a member error") {
    val initialRepoState = GamesRepoState(games = Set(ExampleNotStartedGame), players = Map.empty)
    for {
      repoState     <- Ref.make[GamesRepoState](initialRepoState)
      sentEvents    <- Ref.make[List[InternalMessage]](List.empty)
      result        <- ZIO
                         .accessM[Games](_.get.getInfoAs(ExampleGameId, ExampleUserId))
                         .provideCustomLayer(createLayer(repoState, sentEvents))
                         .either
      updatedRep    <- repoState.get
      updatedEvents <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(ParticipantIsNotAMember(ExampleUserId, ExampleGameId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val readGameWhenNotAccepted = testM("Read game info with participant not accepted error") {
    val initialRepoState =
      GamesRepoState(games = Set(ExampleNotStartedGame), players = Map(ExampleNotStartedGame.id -> Set(ExamplePlayer.copy(role = None))))
    for {
      repoState     <- Ref.make[GamesRepoState](initialRepoState)
      sentEvents    <- Ref.make[List[InternalMessage]](List.empty)
      result        <- ZIO
                         .accessM[Games](_.get.getInfoAs(ExampleGameId, ExampleUserId))
                         .provideCustomLayer(createLayer(repoState, sentEvents))
                         .either
      updatedRep    <- repoState.get
      updatedEvents <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(ParticipantNotAccepted(ExampleGameId, ExamplePlayer.copy(role = None))))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val joinGame = testM("Join game with success") {
    val initialRepoState = GamesRepoState(games = Set(ExampleNotStartedGame))
    for {
      repoState     <- Ref.make[GamesRepoState](initialRepoState)
      sentEvents    <- Ref.make[List[InternalMessage]](List.empty)
      result        <- ZIO
                         .accessM[Games](_.get.joinGame(ExampleGameId, ExampleUserId, ExampleNickName))
                         .provideCustomLayer(createLayer(repoState, sentEvents))
      updatedRep    <- repoState.get
      updatedEvents <- sentEvents.get
    } yield {
      val expectedPlayer = ExamplePlayer.copy(role = None)
      assert(result)(isUnit) &&
      assert(updatedRep)(equalTo(initialRepoState.copy(players = Map(ExampleGameId -> Set(expectedPlayer))))) &&
      assert(updatedEvents)(equalTo(List(InternalMessage.ParticipantJoined(ExampleGameId, ExampleUserId, ExampleNickName))))
    }
  }

  private val joinGameWhenGameNotFound = testM("Join game with game not found error") {
    val initialRepoState = GamesRepoState()
    for {
      repoState     <- Ref.make[GamesRepoState](initialRepoState)
      sentEvents    <- Ref.make[List[InternalMessage]](List.empty)
      result        <- ZIO
                         .accessM[Games](_.get.joinGame(ExampleGameId, ExampleUserId, ExampleNickName))
                         .provideCustomLayer(createLayer(repoState, sentEvents))
                         .either
      updatedRep    <- repoState.get
      updatedEvents <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(GameNotFound(ExampleGameId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val joinGameWhenAlreadyStarted = testM("Join game with game already started error") {
    val initialRepoState = GamesRepoState(games = Set(ExampleGame))
    for {
      repoState     <- Ref.make[GamesRepoState](initialRepoState)
      sentEvents    <- Ref.make[List[InternalMessage]](List.empty)
      result        <- ZIO
                         .accessM[Games](_.get.joinGame(ExampleGameId, ExampleUserId, ExampleNickName))
                         .provideCustomLayer(createLayer(repoState, sentEvents))
                         .either
      updatedRep    <- repoState.get
      updatedEvents <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(GameAlreadyStarted(ExampleGameId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val joinGameWhenAlreadyJoined = testM("Join game with participant already joined error") {
    val initialRepoState = GamesRepoState(games = Set(ExampleNotStartedGame), players = Map(ExampleGameId -> Set(ExamplePlayer)))
    for {
      repoState     <- Ref.make[GamesRepoState](initialRepoState)
      sentEvents    <- Ref.make[List[InternalMessage]](List.empty)
      result        <- ZIO
                         .accessM[Games](_.get.joinGame(ExampleGameId, ExampleUserId, ExampleNickName))
                         .provideCustomLayer(createLayer(repoState, sentEvents))
                         .either
      updatedRep    <- repoState.get
      updatedEvents <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(ParticipantAlreadyJoined(ExampleGameId, ExamplePlayer)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val getParticipants = testM("Get participants with success") {
    val otherParticipant1 = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val otherParticipant2 = Player(ExampleId2, "Ben", None)
    val initialRepoState = GamesRepoState(players = Map(ExampleGameId -> Set(otherParticipant1, otherParticipant2, ExamplePlayer)))
    for {
      repoState  <- Ref.make[GamesRepoState](initialRepoState)
      sentEvents <- Ref.make[List[InternalMessage]](List.empty)
      result     <- ZIO
                      .accessM[Games](_.get.getParticipantsAs(ExampleGameId, ExampleUserId))
                      .provideCustomLayer(createLayer(repoState, sentEvents))
    } yield assert(result)(hasSameElements(List(otherParticipant1, otherParticipant2, ExamplePlayer)))
  }

  private val getParticipantsWhenNotAMember = testM("Get participants with participant is not a member error") {
    val otherParticipant1 = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val otherParticipant2 = Player(ExampleId2, "Ben", None)
    val initialRepoState = GamesRepoState(players = Map(ExampleGameId -> Set(otherParticipant1, otherParticipant2)))
    for {
      repoState  <- Ref.make[GamesRepoState](initialRepoState)
      sentEvents <- Ref.make[List[InternalMessage]](List.empty)
      result     <- ZIO
                      .accessM[Games](_.get.getParticipantsAs(ExampleGameId, ExampleUserId))
                      .provideCustomLayer(createLayer(repoState, sentEvents))
                      .either
    } yield assert(result)(isLeft(equalTo(ParticipantIsNotAMember(ExampleUserId, ExampleGameId))))
  }

  private val getParticipantsWhenNotAccepted = testM("Get participants with participant not accepted error") {
    val otherParticipant1 = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val otherParticipant2 = Player(ExampleId2, "Ben", None)
    val initialRepoState =
      GamesRepoState(players = Map(ExampleGameId -> Set(otherParticipant1, otherParticipant2, ExamplePlayer.copy(role = None))))
    for {
      repoState  <- Ref.make[GamesRepoState](initialRepoState)
      sentEvents <- Ref.make[List[InternalMessage]](List.empty)
      result     <- ZIO
                      .accessM[Games](_.get.getParticipantsAs(ExampleGameId, ExampleUserId))
                      .provideCustomLayer(createLayer(repoState, sentEvents))
                      .either
    } yield assert(result)(isLeft(equalTo(ParticipantNotAccepted(ExampleGameId, ExamplePlayer.copy(role = None)))))
  }

  private val kickParticipant = testM("Kick participant with success") {
    val otherParticipant = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val initialRepoState =
      GamesRepoState(games = Set(ExampleNotStartedGame), players = Map(ExampleGameId -> Set(otherParticipant, ExamplePlayer)))
    for {
      repoState     <- Ref.make[GamesRepoState](initialRepoState)
      sentEvents    <- Ref.make[List[InternalMessage]](List.empty)
      result        <- ZIO
                         .accessM[Games](_.get.kickUserAs(ExampleGameId, otherParticipant.id, ExampleUserId))
                         .provideCustomLayer(createLayer(repoState, sentEvents))
      updatedRep    <- repoState.get
      updatedEvents <- sentEvents.get
    } yield assert(result)(isUnit) &&
      assert(updatedRep)(equalTo(initialRepoState.copy(players = Map(ExampleGameId -> Set(ExamplePlayer))))) &&
      assert(updatedEvents)(equalTo(List(InternalMessage.ParticipantKicked(ExampleGameId, otherParticipant.id))))
  }

  private val kickParticipantWhenGameStarted = testM("Kick participant with game already started error") {
    val otherParticipant = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val initialRepoState =
      GamesRepoState(games = Set(ExampleGame), players = Map(ExampleGameId -> Set(otherParticipant, ExamplePlayer)))
    for {
      repoState     <- Ref.make[GamesRepoState](initialRepoState)
      sentEvents    <- Ref.make[List[InternalMessage]](List.empty)
      result        <- ZIO
                         .accessM[Games](_.get.kickUserAs(ExampleGameId, otherParticipant.id, ExampleUserId))
                         .provideCustomLayer(createLayer(repoState, sentEvents))
                         .either
      updatedRep    <- repoState.get
      updatedEvents <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(GameAlreadyStarted(ExampleGameId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val kickParticipantWhenNotAnOwner = testM("Kick participant with not game owner error") {
    val otherParticipant = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val initialRepoState =
      GamesRepoState(
        games = Set(ExampleNotStartedGame.copy(creator = ExampleId2)),
        players = Map(ExampleGameId -> Set(otherParticipant, ExamplePlayer))
      )
    for {
      repoState     <- Ref.make[GamesRepoState](initialRepoState)
      sentEvents    <- Ref.make[List[InternalMessage]](List.empty)
      result        <- ZIO
                         .accessM[Games](_.get.kickUserAs(ExampleGameId, otherParticipant.id, ExampleUserId))
                         .provideCustomLayer(createLayer(repoState, sentEvents))
                         .either
      updatedRep    <- repoState.get
      updatedEvents <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(UserIsNotGameOwner(ExampleGameId, ExampleUserId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val kickParticipantSelf = testM("Kick participant with kick self forbidden") {
    val otherParticipant = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val initialRepoState =
      GamesRepoState(games = Set(ExampleNotStartedGame), players = Map(ExampleGameId -> Set(otherParticipant, ExamplePlayer)))
    for {
      repoState     <- Ref.make[GamesRepoState](initialRepoState)
      sentEvents    <- Ref.make[List[InternalMessage]](List.empty)
      result        <- ZIO
                         .accessM[Games](_.get.kickUserAs(ExampleGameId, ExampleUserId, ExampleUserId))
                         .provideCustomLayer(createLayer(repoState, sentEvents))
                         .either
      updatedRep    <- repoState.get
      updatedEvents <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(KickSelfForbidden(ExampleGameId, ExampleUserId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val assignRole = testM("Assign role with success") {
    val otherParticipant = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val initialRepoState =
      GamesRepoState(games = Set(ExampleNotStartedGame), players = Map(ExampleGameId -> Set(otherParticipant, ExamplePlayer)))
    for {
      repoState     <- Ref.make[GamesRepoState](initialRepoState)
      sentEvents    <- Ref.make[List[InternalMessage]](List.empty)
      result        <- ZIO
                         .accessM[Games](_.get.assignRoleAs(ExampleGameId, otherParticipant.id, PlayerRole.Player, ExampleUserId))
                         .provideCustomLayer(createLayer(repoState, sentEvents))
      updatedRep    <- repoState.get
      updatedEvents <- sentEvents.get
    } yield assert(result)(isUnit) &&
      assert(updatedRep)(
        equalTo(
          initialRepoState.copy(players = Map(ExampleGameId -> Set(otherParticipant.copy(role = Some(PlayerRole.Player)), ExamplePlayer)))
        )
      ) &&
      assert(updatedEvents)(equalTo(List(InternalMessage.RoleAssigned(ExampleGameId, otherParticipant.id, PlayerRole.Player))))
  }

  private val assignRoleWhenGameStarted = testM("Assign role with game alredy started error") {
    val otherParticipant = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val initialRepoState =
      GamesRepoState(games = Set(ExampleGame), players = Map(ExampleGameId -> Set(otherParticipant, ExamplePlayer)))
    for {
      repoState     <- Ref.make[GamesRepoState](initialRepoState)
      sentEvents    <- Ref.make[List[InternalMessage]](List.empty)
      result        <- ZIO
                         .accessM[Games](_.get.assignRoleAs(ExampleGameId, otherParticipant.id, PlayerRole.Player, ExampleUserId))
                         .provideCustomLayer(createLayer(repoState, sentEvents))
                         .either
      updatedRep    <- repoState.get
      updatedEvents <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(GameAlreadyStarted(ExampleGameId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val assignRoleWhenGameNotFound = testM("Assign role with game not found error") {
    val otherParticipant = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val initialRepoState =
      GamesRepoState(players = Map(ExampleGameId -> Set(otherParticipant, ExamplePlayer)))
    for {
      repoState     <- Ref.make[GamesRepoState](initialRepoState)
      sentEvents    <- Ref.make[List[InternalMessage]](List.empty)
      result        <- ZIO
                         .accessM[Games](_.get.assignRoleAs(ExampleGameId, otherParticipant.id, PlayerRole.Player, ExampleUserId))
                         .provideCustomLayer(createLayer(repoState, sentEvents))
                         .either
      updatedRep    <- repoState.get
      updatedEvents <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(GameNotFound(ExampleGameId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val assignRoleWhenNotAnOwner = testM("Assign role with user is not an owner error") {
    val otherParticipant = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val initialRepoState =
      GamesRepoState(
        games = Set(ExampleNotStartedGame.copy(creator = ExampleId2)),
        players = Map(ExampleGameId -> Set(otherParticipant, ExamplePlayer))
      )
    for {
      repoState     <- Ref.make[GamesRepoState](initialRepoState)
      sentEvents    <- Ref.make[List[InternalMessage]](List.empty)
      result        <- ZIO
                         .accessM[Games](_.get.assignRoleAs(ExampleGameId, otherParticipant.id, PlayerRole.Player, ExampleUserId))
                         .provideCustomLayer(createLayer(repoState, sentEvents))
                         .either
      updatedRep    <- repoState.get
      updatedEvents <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(UserIsNotGameOwner(ExampleGameId, ExampleUserId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

}
