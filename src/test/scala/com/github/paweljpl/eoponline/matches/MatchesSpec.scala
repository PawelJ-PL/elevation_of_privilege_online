package com.github.paweljpl.eoponline.matches

import io.github.gaelrenoux.tranzactio.doobie.{Database => DoobieDb}
import com.github.pawelj_pl.eoponline.`match`.{Card, CardLocation, GameNotFound, GameState, Matches, NotGameMember, Suit, Value}
import com.github.pawelj_pl.eoponline.`match`.Matches.Matches
import com.github.pawelj_pl.eoponline.`match`.dto.ExtendedDeckElementDto
import com.github.paweljpl.eoponline.Constants
import com.github.paweljpl.eoponline.testdoubles.{CardsRepoStub, FakeConnectionSource, FakeGameRepo, FakeGameplayRepo}
import com.github.paweljpl.eoponline.testdoubles.FakeGameRepo.GamesRepoState
import com.github.paweljpl.eoponline.testdoubles.FakeGameplayRepo.{DeckEntry, GameplayRepoState}
import zio.{Ref, ZIO, ZLayer}
import zio.blocking.Blocking
import zio.test.Assertion.{equalTo, hasSameElements, isLeft}
import zio.test.assert
import zio.test.{DefaultRunnableSpec, ZSpec, suite, testM}

object MatchesSpec extends DefaultRunnableSpec with Constants {

  final val Card1 = DeckEntry(ExampleGameId, 1, ExamplePlayer.id, CardLocation.Table, None)

  final val Card2 = DeckEntry(ExampleId2, 2, ExampleId1, CardLocation.Hand, None)

  final val Card3 = DeckEntry(ExampleGameId, 3, ExampleId1, CardLocation.Table, None)

  final val Card4 = DeckEntry(ExampleGameId, 25, ExamplePlayer.id, CardLocation.Hand, None)

  final val Card5 = DeckEntry(ExampleGameId, 32, ExampleId3, CardLocation.Table, Some(true))

  val db: ZLayer[Any with Blocking, Nothing, DoobieDb.Database] =
    FakeConnectionSource.test ++ Blocking.any >>> DoobieDb.fromConnectionSource

  private def createLayer(gamesRepoState: Ref[GamesRepoState], gameplayRepoState: Ref[GameplayRepoState]) =
    db ++ FakeGameRepo.withState(gamesRepoState) ++ FakeGameplayRepo.withState(gameplayRepoState) ++ CardsRepoStub.instance >>> Matches.live

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("Gameplay suite")(getState, getStateWhenStateNotFound, getStateWhenNotMember)

  private val getState = testM("Get match state with success") {
    val cards = Set(Card1, Card2, Card3, Card4, Card5)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> Set(ExamplePlayer, ExamplePlayer.copy(id = ExampleId1), ExamplePlayer.copy(id = ExampleId3)))
    )

    val initialGameplayRepoState = GameplayRepoState(
      gamesState = Set(FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.DenialOfService))),
      decks = cards
    )
    for {
      gamesRepoState    <- Ref.make[GamesRepoState](initialGamesRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](initialGameplayRepoState)
      result            <- ZIO
                             .accessM[Matches](_.get.getCurrentStateForPlayer(ExampleGameId, ExamplePlayer.id))
                             .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState))
    } yield assert(result.state)(equalTo(GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.DenialOfService)))) &&
      assert(result.hand)(hasSameElements(List(Card(25, Value.Ace, Suit.Tampering, "TextQuux")))) &&
      assert(result.table)(
        hasSameElements(
          List(
            ExtendedDeckElementDto(ExampleGameId, ExamplePlayer.id, Card(1, Value.Two, Suit.Spoofing, "TextFoo"), CardLocation.Table, None),
            ExtendedDeckElementDto(ExampleGameId, ExampleId1, Card(3, Value.Four, Suit.Spoofing, "TextBaz"), CardLocation.Table, None),
            ExtendedDeckElementDto(ExampleGameId, ExampleId3, Card(32, Value.Eight, Suit.Repudiation, "TestQuuz"), CardLocation.Table, Some(true))
          )
        )
      )
  }

  private val getStateWhenStateNotFound = testM("Get match state fail when game state not found") {
    val cards = Set(Card1, Card2, Card3, Card4, Card5)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> Set(ExamplePlayer, ExamplePlayer.copy(id = ExampleId1), ExamplePlayer.copy(id = ExampleId3)))
    )

    val initialGameplayRepoState = GameplayRepoState(
      gamesState = Set.empty,
      decks = cards
    )
    for {
      gamesRepoState    <- Ref.make[GamesRepoState](initialGamesRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](initialGameplayRepoState)
      result            <- ZIO
                             .accessM[Matches](_.get.getCurrentStateForPlayer(ExampleGameId, ExamplePlayer.id))
                             .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState))
                             .either
    } yield assert(result)(isLeft(equalTo(GameNotFound(ExampleGameId))))
  }

  private val getStateWhenNotMember = testM("Get match state fail when user is not game member") {
    val cards = Set(Card1, Card2, Card3, Card4, Card5)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> Set(ExamplePlayer, ExamplePlayer.copy(id = ExampleId1), ExamplePlayer.copy(id = ExampleId3)))
    )

    val initialGameplayRepoState = GameplayRepoState(
      gamesState = Set(FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.DenialOfService))),
      decks = cards
    )
    for {
      gamesRepoState    <- Ref.make[GamesRepoState](initialGamesRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](initialGameplayRepoState)
      result            <- ZIO
                             .accessM[Matches](_.get.getCurrentStateForPlayer(ExampleGameId, ExampleId4))
                             .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState))
                             .either
    } yield assert(result)(isLeft(equalTo(NotGameMember(ExampleGameId, ExampleId4))))
  }

}
