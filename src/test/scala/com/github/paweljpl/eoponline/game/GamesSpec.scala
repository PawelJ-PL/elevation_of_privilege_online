package com.github.paweljpl.eoponline.game

import java.time.Instant

import cats.syntax.eq._
import cats.instances.int._
import com.github.pawelj_pl.eoponline.eventbus.InternalMessage
import com.github.pawelj_pl.eoponline.eventbus.InternalMessage.{GameStarted, ParticipantKicked}
import com.github.pawelj_pl.eoponline.game.{
  GameAlreadyFinished,
  GameAlreadyStarted,
  GameNotFound,
  Games,
  KickSelfForbidden,
  NotEnoughPlayers,
  ParticipantAlreadyJoined,
  ParticipantIsNotAMember,
  ParticipantNotAccepted,
  Player,
  PlayerRole,
  TooManyPlayers,
  UserIsNotGameOwner
}
import com.github.pawelj_pl.eoponline.game.Games.Games
import com.github.pawelj_pl.eoponline.game.dto.NewGameDto
import com.github.pawelj_pl.eoponline.`match`.{CardLocation, Suit}
import com.github.paweljpl.eoponline.Constants
import com.github.paweljpl.eoponline.testdoubles.{
  CardsRepoStub,
  FakeClock,
  FakeGameRepo,
  FakeGameplayRepo,
  FakeInternalMessagesTopic,
  RandomMock
}
import com.github.paweljpl.eoponline.testdoubles.FakeGameRepo.GamesRepoState
import com.github.paweljpl.eoponline.testdoubles.FakeGameplayRepo.{DeckEntry, GameState, GameplayRepoState}
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

  val db: ZLayer[Any with Blocking, Nothing, DoobieDb.Database] = DoobieDb.none

  private def createLayer(
    gamesRepoState: Ref[GamesRepoState],
    sentEvents: Ref[List[InternalMessage]],
    gameplayRepoState: Ref[GameplayRepoState]
  ) =
    FakeGameRepo.withState(gamesRepoState) ++ logging ++ FakeInternalMessagesTopic.test(
      sentEvents
    ) ++ db ++ RandomMock.test ++ FakeGameplayRepo.withState(
      gameplayRepoState
    ) ++ CardsRepoStub.instance ++ FakeClock.instance >>> Games.live

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("Game setup suite")(
      createGame,
      readGame,
      readMissingGame,
      readGameWhenNotMember,
      readGameWhenNotAccepted,
      readAlreadyStartedGameWhenNotAccepted,
      readAlreadyStartedGameWhenAccepted,
      joinGame,
      joinGameWhenGameNotFound,
      joinGameWhenAlreadyStarted,
      joinGameWhenAlreadyFinished,
      joinGameWhenAlreadyJoined,
      getParticipants,
      getParticipantsWhenNotAMember,
      getParticipantsWhenNotAccepted,
      kickParticipant,
      kickParticipantWhenGameStarted,
      kickParticipantWhenGameFinished,
      kickParticipantWhenNotAnOwner,
      kickParticipantSelf,
      assignRole,
      assignRoleWhenGameStarted,
      assignRoleWhenGameNotFound,
      assignRoleWhenNotAnOwner,
      startGame,
      startGameWithNonAcceptedUsers,
      startNonExistingGame,
      startGameWhenNotAnOwner,
      startAlreadyStartedGame,
      startFinishedGame,
      startGameWithNotEnoughPlayers,
      startGameWithTooManyPlayers,
      assignRoleWhenGameFinished
    )

  private val newGameDto = NewGameDto(Some(ExampleGameDescription), ExampleNickName)

  private val createGame =
    testM("Create game with success")(
      for {
        repoState         <- Ref.make[GamesRepoState](GamesRepoState())
        gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
        sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
        game              <- ZIO
                               .accessM[Games](_.get.create(newGameDto, ExampleUserId))
                               .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
        updatedRep        <- repoState.get
        updatedEvents     <- sentEvents.get
      } yield {
        val expectedGame = ExampleNotStartedGame.copy(id = FirstRandomFuuid)
        val expectedEvent = InternalMessage.GameCreated(expectedGame)
        assert(game)(equalTo(expectedGame)) &&
        assert(updatedRep)(equalTo(GamesRepoState(Set(expectedGame), Map(FirstRandomFuuid -> List(ExamplePlayer))))) &&
        assert(updatedEvents)(equalTo(List(expectedEvent)))
      }
    )

  private val readGame = testM("Read game info with success") {
    val initialRepoState =
      GamesRepoState(games = Set(ExampleNotStartedGame), players = Map(ExampleNotStartedGame.id -> List(ExamplePlayer)))
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      game              <- ZIO
                             .accessM[Games](_.get.getInfoAs(ExampleGameId, ExampleUserId))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
      updatedRep        <- repoState.get
      updatedEvents     <- sentEvents.get
    } yield assert(game)(equalTo(ExampleNotStartedGame)) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val readMissingGame = testM("Read game info with Game not found error") {
    val initialRepoState = GamesRepoState(games = Set.empty, players = Map(ExampleNotStartedGame.id -> List(ExamplePlayer)))
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.getInfoAs(ExampleGameId, ExampleUserId))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                             .either
      updatedRep        <- repoState.get
      updatedEvents     <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(GameNotFound(ExampleGameId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val readGameWhenNotMember = testM("Read game info with user is not a member error") {
    val initialRepoState = GamesRepoState(games = Set(ExampleNotStartedGame), players = Map.empty)
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.getInfoAs(ExampleGameId, ExampleUserId))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                             .either
      updatedRep        <- repoState.get
      updatedEvents     <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(ParticipantIsNotAMember(ExampleUserId, ExampleGameId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val readGameWhenNotAccepted = testM("Read game info with participant not accepted error") {
    val initialRepoState =
      GamesRepoState(games = Set(ExampleNotStartedGame), players = Map(ExampleNotStartedGame.id -> List(ExamplePlayer.copy(role = None))))
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.getInfoAs(ExampleGameId, ExampleUserId))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                             .either
      updatedRep        <- repoState.get
      updatedEvents     <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(ParticipantNotAccepted(ExampleGameId, ExamplePlayer.copy(role = None))))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val readAlreadyStartedGameWhenNotAccepted = testM("Read game info with success") {
    val initialRepoState =
      GamesRepoState(games = Set(ExampleGame), players = Map(ExampleGame.id -> List(ExamplePlayer)))
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.getInfoAs(ExampleGameId, ExampleId2))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                             .either
      updatedRep        <- repoState.get
      updatedEvents     <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(GameAlreadyStarted(ExampleGame.id)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val readAlreadyStartedGameWhenAccepted = testM("Read game info with success") {
    val initialRepoState =
      GamesRepoState(games = Set(ExampleGame), players = Map(ExampleGame.id -> List(ExamplePlayer)))
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      game              <- ZIO
                             .accessM[Games](_.get.getInfoAs(ExampleGameId, ExampleUserId))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
      updatedRep        <- repoState.get
      updatedEvents     <- sentEvents.get
    } yield assert(game)(equalTo(ExampleGame)) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val joinGame = testM("Join game with success") {
    val initialRepoState = GamesRepoState(games = Set(ExampleNotStartedGame))
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.joinGame(ExampleGameId, ExampleUserId, ExampleNickName))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
      updatedRep        <- repoState.get
      updatedEvents     <- sentEvents.get
    } yield {
      val expectedPlayer = ExamplePlayer.copy(role = None)
      assert(result)(isUnit) &&
      assert(updatedRep)(equalTo(initialRepoState.copy(players = Map(ExampleGameId -> List(expectedPlayer))))) &&
      assert(updatedEvents)(equalTo(List(InternalMessage.ParticipantJoined(ExampleGameId, ExampleUserId, ExampleNickName))))
    }
  }

  private val joinGameWhenGameNotFound = testM("Join game with game not found error") {
    val initialRepoState = GamesRepoState()
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.joinGame(ExampleGameId, ExampleUserId, ExampleNickName))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                             .either
      updatedRep        <- repoState.get
      updatedEvents     <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(GameNotFound(ExampleGameId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val joinGameWhenAlreadyStarted = testM("Join game with game already started error") {
    val initialRepoState = GamesRepoState(games = Set(ExampleGame))
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.joinGame(ExampleGameId, ExampleUserId, ExampleNickName))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                             .either
      updatedRep        <- repoState.get
      updatedEvents     <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(GameAlreadyStarted(ExampleGameId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val joinGameWhenAlreadyFinished = testM("Join game with game already finished error") {
    val initialRepoState = GamesRepoState(games = Set(ExampleNotStartedGame.copy(finishedAt = Some(Now))))
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.joinGame(ExampleGameId, ExampleUserId, ExampleNickName))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                             .either
      updatedRep        <- repoState.get
      updatedEvents     <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(GameAlreadyFinished(ExampleGameId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val joinGameWhenAlreadyJoined = testM("Join game with participant already joined error") {
    val initialRepoState = GamesRepoState(games = Set(ExampleNotStartedGame), players = Map(ExampleGameId -> List(ExamplePlayer)))
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.joinGame(ExampleGameId, ExampleUserId, ExampleNickName))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                             .either
      updatedRep        <- repoState.get
      updatedEvents     <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(ParticipantAlreadyJoined(ExampleGameId, ExamplePlayer)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val getParticipants = testM("Get participants with success") {
    val otherParticipant1 = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val otherParticipant2 = Player(ExampleId2, "Ben", None)
    val initialRepoState = GamesRepoState(players = Map(ExampleGameId -> List(otherParticipant1, otherParticipant2, ExamplePlayer)))
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.getParticipantsAs(ExampleGameId, ExampleUserId))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
    } yield assert(result)(hasSameElements(List(otherParticipant1, otherParticipant2, ExamplePlayer)))
  }

  private val getParticipantsWhenNotAMember = testM("Get participants with participant is not a member error") {
    val otherParticipant1 = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val otherParticipant2 = Player(ExampleId2, "Ben", None)
    val initialRepoState = GamesRepoState(players = Map(ExampleGameId -> List(otherParticipant1, otherParticipant2)))
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.getParticipantsAs(ExampleGameId, ExampleUserId))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                             .either
    } yield assert(result)(isLeft(equalTo(ParticipantIsNotAMember(ExampleUserId, ExampleGameId))))
  }

  private val getParticipantsWhenNotAccepted = testM("Get participants with participant not accepted error") {
    val otherParticipant1 = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val otherParticipant2 = Player(ExampleId2, "Ben", None)
    val initialRepoState =
      GamesRepoState(players = Map(ExampleGameId -> List(otherParticipant1, otherParticipant2, ExamplePlayer.copy(role = None))))
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.getParticipantsAs(ExampleGameId, ExampleUserId))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                             .either
    } yield assert(result)(isLeft(equalTo(ParticipantNotAccepted(ExampleGameId, ExamplePlayer.copy(role = None)))))
  }

  private val kickParticipant = testM("Kick participant with success") {
    val otherParticipant = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val initialRepoState =
      GamesRepoState(games = Set(ExampleNotStartedGame), players = Map(ExampleGameId -> List(otherParticipant, ExamplePlayer)))
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.kickUserAs(ExampleGameId, otherParticipant.id, ExampleUserId))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
      updatedRep        <- repoState.get
      updatedEvents     <- sentEvents.get
    } yield assert(result)(isUnit) &&
      assert(updatedRep)(equalTo(initialRepoState.copy(players = Map(ExampleGameId -> List(ExamplePlayer))))) &&
      assert(updatedEvents)(equalTo(List(InternalMessage.ParticipantKicked(ExampleGameId, otherParticipant.id))))
  }

  private val kickParticipantWhenGameStarted = testM("Kick participant with game already started error") {
    val otherParticipant = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val initialRepoState =
      GamesRepoState(games = Set(ExampleGame), players = Map(ExampleGameId -> List(otherParticipant, ExamplePlayer)))
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.kickUserAs(ExampleGameId, otherParticipant.id, ExampleUserId))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                             .either
      updatedRep        <- repoState.get
      updatedEvents     <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(GameAlreadyStarted(ExampleGameId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val kickParticipantWhenGameFinished = testM("Kick participant with game already finished error") {
    val otherParticipant = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val initialRepoState =
      GamesRepoState(
        games = Set(ExampleNotStartedGame.copy(finishedAt = Some(Now))),
        players = Map(ExampleGameId -> List(otherParticipant, ExamplePlayer))
      )
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.kickUserAs(ExampleGameId, otherParticipant.id, ExampleUserId))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                             .either
      updatedRep        <- repoState.get
      updatedEvents     <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(GameAlreadyFinished(ExampleGameId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val kickParticipantWhenNotAnOwner = testM("Kick participant with not game owner error") {
    val otherParticipant = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val initialRepoState =
      GamesRepoState(
        games = Set(ExampleNotStartedGame.copy(creator = ExampleId2)),
        players = Map(ExampleGameId -> List(otherParticipant, ExamplePlayer))
      )
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.kickUserAs(ExampleGameId, otherParticipant.id, ExampleUserId))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                             .either
      updatedRep        <- repoState.get
      updatedEvents     <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(UserIsNotGameOwner(ExampleGameId, ExampleUserId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val kickParticipantSelf = testM("Kick participant with kick self forbidden") {
    val otherParticipant = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val initialRepoState =
      GamesRepoState(games = Set(ExampleNotStartedGame), players = Map(ExampleGameId -> List(otherParticipant, ExamplePlayer)))
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.kickUserAs(ExampleGameId, ExampleUserId, ExampleUserId))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                             .either
      updatedRep        <- repoState.get
      updatedEvents     <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(KickSelfForbidden(ExampleGameId, ExampleUserId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val assignRole = testM("Assign role with success") {
    val otherParticipant = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val initialRepoState =
      GamesRepoState(games = Set(ExampleNotStartedGame), players = Map(ExampleGameId -> List(otherParticipant, ExamplePlayer)))
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.assignRoleAs(ExampleGameId, otherParticipant.id, PlayerRole.Player, ExampleUserId))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
      updatedRep        <- repoState.get
      updatedEvents     <- sentEvents.get
    } yield assert(result)(isUnit) &&
      assert(updatedRep)(
        equalTo(
          initialRepoState.copy(players = Map(ExampleGameId -> List(otherParticipant.copy(role = Some(PlayerRole.Player)), ExamplePlayer)))
        )
      ) &&
      assert(updatedEvents)(equalTo(List(InternalMessage.RoleAssigned(ExampleGameId, otherParticipant.id, PlayerRole.Player))))
  }

  private val assignRoleWhenGameStarted = testM("Assign role with game already started error") {
    val otherParticipant = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val initialRepoState =
      GamesRepoState(games = Set(ExampleGame), players = Map(ExampleGameId -> List(otherParticipant, ExamplePlayer)))
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.assignRoleAs(ExampleGameId, otherParticipant.id, PlayerRole.Player, ExampleUserId))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                             .either
      updatedRep        <- repoState.get
      updatedEvents     <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(GameAlreadyStarted(ExampleGameId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val assignRoleWhenGameFinished = testM("Assign role with game already started error") {
    val otherParticipant = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val initialRepoState =
      GamesRepoState(
        games = Set(ExampleNotStartedGame.copy(finishedAt = Some(Now))),
        players = Map(ExampleGameId -> List(otherParticipant, ExamplePlayer))
      )
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.assignRoleAs(ExampleGameId, otherParticipant.id, PlayerRole.Player, ExampleUserId))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                             .either
      updatedRep        <- repoState.get
      updatedEvents     <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(GameAlreadyFinished(ExampleGameId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val assignRoleWhenGameNotFound = testM("Assign role with game not found error") {
    val otherParticipant = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val initialRepoState =
      GamesRepoState(players = Map(ExampleGameId -> List(otherParticipant, ExamplePlayer)))
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.assignRoleAs(ExampleGameId, otherParticipant.id, PlayerRole.Player, ExampleUserId))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                             .either
      updatedRep        <- repoState.get
      updatedEvents     <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(GameNotFound(ExampleGameId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val assignRoleWhenNotAnOwner = testM("Assign role with user is not an owner error") {
    val otherParticipant = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val initialRepoState =
      GamesRepoState(
        games = Set(ExampleNotStartedGame.copy(creator = ExampleId2)),
        players = Map(ExampleGameId -> List(otherParticipant, ExamplePlayer))
      )
    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](GameplayRepoState())
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.assignRoleAs(ExampleGameId, otherParticipant.id, PlayerRole.Player, ExampleUserId))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                             .either
      updatedRep        <- repoState.get
      updatedEvents     <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(UserIsNotGameOwner(ExampleGameId, ExampleUserId)))) &&
      assert(updatedRep)(equalTo(initialRepoState)) &&
      assert(updatedEvents)(isEmpty)
  }

  private val startGame = testM("Start game with success") {
    val player2 = Player(ExampleId1, "Adam", Some(PlayerRole.Player))
    val initialRepoState = GamesRepoState(
      games = Set(ExampleNotStartedGame),
      players = Map(ExampleGameId -> List(ExamplePlayer, player2))
    )
    val initialGameplayRepoState = GameplayRepoState()
    val expectedPlayer1Cards =
      (2 to 74 by 2).map(cardNumber => DeckEntry(ExampleGameId, cardNumber, ExamplePlayer.id, CardLocation.Hand, None)).toSet
    val expectedPlayer2Cards = (1 to 74 by 2).map(cardNumber => DeckEntry(ExampleGameId, cardNumber, ExampleId1, CardLocation.Hand, None))
    val expectedAllCards = (expectedPlayer1Cards ++ expectedPlayer2Cards).map(deck =>
      if (deck.cardNumber =!= 14) deck else DeckEntry(deck.gameId, 14, deck.playerId, CardLocation.Table, None) // 14 is Three of Tampering
    )

    for {
      repoState           <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState   <- Ref.make[GameplayRepoState](initialGameplayRepoState)
      sentEvents          <- Ref.make[List[InternalMessage]](List.empty)
      result              <- ZIO
                               .accessM[Games](_.get.startGameAs(ExampleGameId, ExamplePlayer.id))
                               .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
      updatedRepo         <- repoState.get
      updatedEvents       <- sentEvents.get
      updatedGameplayRepo <- gameplayRepoState.get
    } yield assert(result)(equalTo(())) &&
      assert(updatedRepo)(
        equalTo(initialRepoState.copy(games = Set(ExampleNotStartedGame.copy(startedAt = Some(Instant.ofEpochMilli(InitClockMillis))))))
      ) &&
      assert(updatedEvents)(equalTo(List(GameStarted(ExampleGameId)))) &&
      assert(updatedGameplayRepo.decks)(hasSameElements(expectedAllCards)) &&
      assert(updatedGameplayRepo.gamesState)(hasSameElements(Set(GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Tampering)))))
  }

  private val startGameWithNonAcceptedUsers = testM("Remove not accepted participants on start") {
    val player2 = Player(ExampleId1, "P1", Some(PlayerRole.Player))
    val player3 = Player(ExampleId2, "P2", None)
    val player4 = Player(ExampleId3, "P3", Some(PlayerRole.Observer))
    val player5 = Player(ExampleId4, "P4", None)
    val initialRepoState = GamesRepoState(
      games = Set(ExampleNotStartedGame),
      players = Map(ExampleGameId -> List(ExamplePlayer, player2, player3, player4, player5))
    )
    val expectedPlayers =
      Map(
        ExampleGameId -> List(
          ExamplePlayer,
          Player(ExampleId1, "P1", Some(PlayerRole.Player)),
          Player(ExampleId3, "P3", Some(PlayerRole.Observer))
        )
      )
    val initialGameplayRepoState = GameplayRepoState()

    for {
      repoState         <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](initialGameplayRepoState)
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Games](_.get.startGameAs(ExampleGameId, ExamplePlayer.id))
                             .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
      updatedRepo       <- repoState.get
      updatedEvents     <- sentEvents.get
    } yield assert(result)(equalTo(())) &&
      assert(updatedRepo)(
        equalTo(
          initialRepoState.copy(
            games = Set(ExampleNotStartedGame.copy(startedAt = Some(Instant.ofEpochMilli(InitClockMillis)))),
            players = expectedPlayers
          )
        )
      ) &&
      assert(updatedEvents)(
        hasSameElements(
          List(GameStarted(ExampleGameId), ParticipantKicked(ExampleGameId, ExampleId2), ParticipantKicked(ExampleGameId, ExampleId4))
        )
      )
  }

  private val startNonExistingGame = testM("Start not created game") {
    val player2 = Player(ExampleId1, "Adam", Some(PlayerRole.Player))
    val initialRepoState = GamesRepoState(
      games = Set.empty,
      players = Map(ExampleGameId -> List(ExamplePlayer, player2))
    )
    val initialGameplayRepoState = GameplayRepoState()

    for {
      repoState           <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState   <- Ref.make[GameplayRepoState](initialGameplayRepoState)
      sentEvents          <- Ref.make[List[InternalMessage]](List.empty)
      result              <- ZIO
                               .accessM[Games](_.get.startGameAs(ExampleGameId, ExamplePlayer.id))
                               .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                               .either
      updatedRepo         <- repoState.get
      updatedEvents       <- sentEvents.get
      updatedGameplayRepo <- gameplayRepoState.get
    } yield assert(result)(isLeft(equalTo(GameNotFound(ExampleGameId)))) &&
      assert(updatedRepo)(equalTo(initialRepoState)) &&
      assert(updatedGameplayRepo)(equalTo(initialGameplayRepoState)) &&
      assert(updatedEvents)(equalTo(List.empty))
  }

  private val startGameWhenNotAnOwner = testM("Start game owned by other player") {
    val player2 = Player(ExampleId1, "Adam", Some(PlayerRole.Player))
    val initialRepoState = GamesRepoState(
      games = Set(ExampleNotStartedGame),
      players = Map(ExampleGameId -> List(ExamplePlayer, player2))
    )
    val initialGameplayRepoState = GameplayRepoState()

    for {
      repoState           <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState   <- Ref.make[GameplayRepoState](initialGameplayRepoState)
      sentEvents          <- Ref.make[List[InternalMessage]](List.empty)
      result              <- ZIO
                               .accessM[Games](_.get.startGameAs(ExampleGameId, ExampleId1))
                               .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                               .either
      updatedRepo         <- repoState.get
      updatedEvents       <- sentEvents.get
      updatedGameplayRepo <- gameplayRepoState.get
    } yield assert(result)(isLeft(equalTo(UserIsNotGameOwner(ExampleGameId, ExampleId1)))) &&
      assert(updatedRepo)(equalTo(initialRepoState)) &&
      assert(updatedGameplayRepo)(equalTo(initialGameplayRepoState)) &&
      assert(updatedEvents)(equalTo(List.empty))
  }

  private val startAlreadyStartedGame = testM("Start already started game") {
    val player2 = Player(ExampleId1, "Adam", Some(PlayerRole.Player))
    val initialRepoState = GamesRepoState(
      games = Set(ExampleGame),
      players = Map(ExampleGameId -> List(ExamplePlayer, player2))
    )
    val initialGameplayRepoState = GameplayRepoState()

    for {
      repoState           <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState   <- Ref.make[GameplayRepoState](initialGameplayRepoState)
      sentEvents          <- Ref.make[List[InternalMessage]](List.empty)
      result              <- ZIO
                               .accessM[Games](_.get.startGameAs(ExampleGameId, ExamplePlayer.id))
                               .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                               .either
      updatedRepo         <- repoState.get
      updatedEvents       <- sentEvents.get
      updatedGameplayRepo <- gameplayRepoState.get
    } yield assert(result)(isLeft(equalTo(GameAlreadyStarted(ExampleGameId)))) &&
      assert(updatedRepo)(equalTo(initialRepoState)) &&
      assert(updatedGameplayRepo)(equalTo(initialGameplayRepoState)) &&
      assert(updatedEvents)(equalTo(List.empty))
  }

  private val startFinishedGame = testM("Start finished game") {
    val player2 = Player(ExampleId1, "Adam", Some(PlayerRole.Player))
    val initialRepoState = GamesRepoState(
      games = Set(ExampleNotStartedGame.copy(finishedAt = Some(Now))),
      players = Map(ExampleGameId -> List(ExamplePlayer, player2))
    )
    val initialGameplayRepoState = GameplayRepoState()

    for {
      repoState           <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState   <- Ref.make[GameplayRepoState](initialGameplayRepoState)
      sentEvents          <- Ref.make[List[InternalMessage]](List.empty)
      result              <- ZIO
                               .accessM[Games](_.get.startGameAs(ExampleGameId, ExamplePlayer.id))
                               .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                               .either
      updatedRepo         <- repoState.get
      updatedEvents       <- sentEvents.get
      updatedGameplayRepo <- gameplayRepoState.get
    } yield assert(result)(isLeft(equalTo(GameAlreadyFinished(ExampleGameId)))) &&
      assert(updatedRepo)(equalTo(initialRepoState)) &&
      assert(updatedGameplayRepo)(equalTo(initialGameplayRepoState)) &&
      assert(updatedEvents)(equalTo(List.empty))
  }

  private val startGameWithNotEnoughPlayers = testM("Start game with not enough players") {
    val player2 = Player(ExampleId1, "Adam", Some(PlayerRole.Observer))
    val initialRepoState = GamesRepoState(
      games = Set(ExampleNotStartedGame),
      players = Map(ExampleGameId -> List(ExamplePlayer, player2))
    )
    val initialGameplayRepoState = GameplayRepoState()

    for {
      repoState           <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState   <- Ref.make[GameplayRepoState](initialGameplayRepoState)
      sentEvents          <- Ref.make[List[InternalMessage]](List.empty)
      result              <- ZIO
                               .accessM[Games](_.get.startGameAs(ExampleGameId, ExamplePlayer.id))
                               .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                               .either
      updatedRepo         <- repoState.get
      updatedEvents       <- sentEvents.get
      updatedGameplayRepo <- gameplayRepoState.get
    } yield assert(result)(isLeft(equalTo(NotEnoughPlayers(ExampleGameId, 1)))) &&
      assert(updatedRepo)(equalTo(initialRepoState)) &&
      assert(updatedGameplayRepo)(equalTo(initialGameplayRepoState)) &&
      assert(updatedEvents)(equalTo(List.empty))
  }

  private val startGameWithTooManyPlayers = testM("Start game with too many players") {
    val player2 = Player(ExampleId1, "P2", Some(PlayerRole.Player))
    val player3 = Player(ExampleId2, "P3", Some(PlayerRole.Player))
    val player4 = Player(ExampleId3, "P4", Some(PlayerRole.Player))
    val player5 = Player(ExampleId4, "P5", Some(PlayerRole.Player))
    val player6 = Player(ExampleId5, "P6", Some(PlayerRole.Player))
    val player7 = Player(ExampleId6, "P7", Some(PlayerRole.Player))
    val player8 = Player(ExampleId7, "P8", Some(PlayerRole.Player))
    val player9 = Player(ExampleId8, "P9", Some(PlayerRole.Player))
    val player10 = Player(ExampleId9, "P10", Some(PlayerRole.Player))
    val player11 = Player(ExampleId10, "P11", Some(PlayerRole.Player))
    val initialRepoState = GamesRepoState(
      games = Set(ExampleNotStartedGame),
      players = Map(
        ExampleGameId -> List(ExamplePlayer, player2, player3, player4, player5, player6, player7, player8, player9, player10, player11)
      )
    )
    val initialGameplayRepoState = GameplayRepoState()

    for {
      repoState           <- Ref.make[GamesRepoState](initialRepoState)
      gameplayRepoState   <- Ref.make[GameplayRepoState](initialGameplayRepoState)
      sentEvents          <- Ref.make[List[InternalMessage]](List.empty)
      result              <- ZIO
                               .accessM[Games](_.get.startGameAs(ExampleGameId, ExamplePlayer.id))
                               .provideCustomLayer(createLayer(repoState, sentEvents, gameplayRepoState))
                               .either
      updatedRepo         <- repoState.get
      updatedEvents       <- sentEvents.get
      updatedGameplayRepo <- gameplayRepoState.get
    } yield assert(result)(isLeft(equalTo(TooManyPlayers(ExampleGameId, 11)))) &&
      assert(updatedRepo)(equalTo(initialRepoState)) &&
      assert(updatedGameplayRepo)(equalTo(initialGameplayRepoState)) &&
      assert(updatedEvents)(equalTo(List.empty))
  }

}
