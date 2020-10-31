package com.github.paweljpl.eoponline.matches

import io.github.gaelrenoux.tranzactio.doobie.{Database => DoobieDb}
import com.github.pawelj_pl.eoponline.`match`.{
  Card,
  CardLocation,
  CardNotFound,
  CardOwnedByAnotherUser,
  GameNotFound,
  GameState,
  Matches,
  NotGameMember,
  NotPlayer,
  OtherPlayersTurn,
  PlayerAlreadyPlayedCard,
  Suit,
  SuitDoesNotMatch,
  ThreatStatusAlreadyAssigned,
  UnexpectedCardLocation,
  Value
}
import com.github.pawelj_pl.eoponline.`match`.Matches.Matches
import com.github.pawelj_pl.eoponline.`match`.dto.{ExtendedDeckElementDto, TableCardReqDto}
import com.github.pawelj_pl.eoponline.eventbus.InternalMessage
import com.github.pawelj_pl.eoponline.game.PlayerRole
import com.github.paweljpl.eoponline.Constants
import com.github.paweljpl.eoponline.testdoubles.{
  CardsRepoStub,
  FakeClock,
  FakeConnectionSource,
  FakeGameRepo,
  FakeGameplayRepo,
  FakeInternalMessagesTopic,
  RandomMock
}
import com.github.paweljpl.eoponline.testdoubles.FakeGameRepo.GamesRepoState
import com.github.paweljpl.eoponline.testdoubles.FakeGameplayRepo.{DeckEntry, GameplayRepoState}
import zio.{Ref, ZIO, ZLayer}
import zio.blocking.Blocking
import zio.test.Assertion.{equalTo, hasSameElements, isEmpty, isLeft}
import zio.test.assert
import zio.test.{DefaultRunnableSpec, ZSpec, suite, testM}

object MatchesSpec extends DefaultRunnableSpec with Constants {

  final val Card1 = DeckEntry(ExampleGameId, 1, ExamplePlayer.id, CardLocation.Table, None)

  final val Card2 = DeckEntry(ExampleId2, 2, ExampleId1, CardLocation.Hand, None)

  final val Card3 = DeckEntry(ExampleGameId, 3, ExampleId1, CardLocation.Table, None)

  final val Card4 = DeckEntry(ExampleGameId, 25, ExamplePlayer.id, CardLocation.Hand, None)

  final val Card5 = DeckEntry(ExampleGameId, 32, ExampleId3, CardLocation.Table, Some(true))

  final val UpdateTableDto = TableCardReqDto(true)

  val db: ZLayer[Any with Blocking, Nothing, DoobieDb.Database] =
    FakeConnectionSource.test ++ Blocking.any >>> DoobieDb.fromConnectionSource

  private def createLayer(
    gamesRepoState: Ref[GamesRepoState],
    gameplayRepoState: Ref[GameplayRepoState],
    sentEvents: Ref[List[InternalMessage]]
  ) =
    db ++ FakeGameRepo.withState(gamesRepoState) ++ FakeGameplayRepo.withState(
      gameplayRepoState
    ) ++ CardsRepoStub.instance ++ FakeInternalMessagesTopic.test(sentEvents) ++ RandomMock.test ++ FakeClock.instance >>> Matches.live

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("Gameplay suite")(
      getState,
      getStateWhenStateNotFound,
      getStateWhenNotMember,
      linkThreadAndFinishGame,
      linkThreadAndGoToNextPlayer,
      linkThreadAndFinishTurn,
      linkThreadWhenGameNotFound,
      linkThreadWhenNotAMember,
      linkThreadWhenNotPlayerRole,
      linkThreadWhenOtherPlayersTurn,
      linkThreadWhenCardDoesNotExistInDeck,
      linkThreadWhenCardsIsOwnedByAnotherPlayer,
      linkThreadWhenCardNotOnTheTable,
      linkThreadWhenThreadAlreadyAssigned,
      linkThreadSelectPlayerForward,
      linkThreadSelectPlayerBackward,
      linkThreadAndRecordTrick,
      notTakeTheTrickWhenThreatNotLinked,
      takeTheTrickWhenTrumpCardPlayed,
      doNotRecordTheTrickWhenNobodyWins,
      selectWinnerForNextTurnInitPlayer,
      selectSecondWinnerForNextTurnInitPlayerIfWinnerHasNoCardsOnHand,
      selectRandomNextTurnInitPlayerIfNoWinner,
      playTheCardWithSameSuit,
      playTheCardWhenNoMatchingCardOnHand,
      playTheCardWhenNoLeadingSuit,
      playTheCardWithNotMatchingSuit,
      playTheCardWhenGameNotFound,
      playTheCardWhenUserIsNotGameMember,
      playTheCardWhenUserIsNotPlayer,
      playTheCardWhenCardNotFound,
      playTheCardWhenOtherPlayersTurn,
      playTheCardWhenCardOwnedByAnotherUser,
      playTheCardWhenCardIsNotOnTheHand,
      playTheCardWhenPlayerAlreadyPlayedAnotherCard
    )

  private val getState = testM("Get match state with success") {
    val cards = Set(Card1, Card2, Card3, Card4, Card5)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer, ExamplePlayer.copy(id = ExampleId1), ExamplePlayer.copy(id = ExampleId3)))
    )

    val initialGameplayRepoState = GameplayRepoState(
      gamesState = Set(FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.DenialOfService))),
      decks = cards
    )
    for {
      gamesRepoState    <- Ref.make[GamesRepoState](initialGamesRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](initialGameplayRepoState)
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Matches](_.get.getCurrentStateForPlayer(ExampleGameId, ExamplePlayer.id))
                             .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
    } yield assert(result.state)(equalTo(GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.DenialOfService)))) &&
      assert(result.hand)(hasSameElements(List(Card(25, Value.Ace, Suit.Tampering, "TextQuux")))) &&
      assert(result.table)(
        hasSameElements(
          List(
            ExtendedDeckElementDto(ExampleGameId, ExamplePlayer.id, Card(1, Value.Two, Suit.Spoofing, "TextFoo"), CardLocation.Table, None),
            ExtendedDeckElementDto(ExampleGameId, ExampleId1, Card(3, Value.Four, Suit.Spoofing, "TextBaz"), CardLocation.Table, None),
            ExtendedDeckElementDto(
              ExampleGameId,
              ExampleId3,
              Card(32, Value.Eight, Suit.Repudiation, "TestQuuz"),
              CardLocation.Table,
              Some(true)
            )
          )
        )
      )
  }

  private val getStateWhenStateNotFound = testM("Get match state fail when game state not found") {
    val cards = Set(Card1, Card2, Card3, Card4, Card5)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer, ExamplePlayer.copy(id = ExampleId1), ExamplePlayer.copy(id = ExampleId3)))
    )

    val initialGameplayRepoState = GameplayRepoState(
      gamesState = Set.empty,
      decks = cards
    )
    for {
      gamesRepoState    <- Ref.make[GamesRepoState](initialGamesRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](initialGameplayRepoState)
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Matches](_.get.getCurrentStateForPlayer(ExampleGameId, ExamplePlayer.id))
                             .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
                             .either
    } yield assert(result)(isLeft(equalTo(GameNotFound(ExampleGameId))))
  }

  private val getStateWhenNotMember = testM("Get match state fail when user is not game member") {
    val cards = Set(Card1, Card2, Card3, Card4, Card5)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer, ExamplePlayer.copy(id = ExampleId1), ExamplePlayer.copy(id = ExampleId3)))
    )

    val initialGameplayRepoState = GameplayRepoState(
      gamesState = Set(FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.DenialOfService))),
      decks = cards
    )
    for {
      gamesRepoState    <- Ref.make[GamesRepoState](initialGamesRepoState)
      gameplayRepoState <- Ref.make[GameplayRepoState](initialGameplayRepoState)
      sentEvents        <- Ref.make[List[InternalMessage]](List.empty)
      result            <- ZIO
                             .accessM[Matches](_.get.getCurrentStateForPlayer(ExampleGameId, ExampleId4))
                             .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
                             .either
    } yield assert(result)(isLeft(equalTo(NotGameMember(ExampleGameId, ExampleId4))))
  }

  private val linkThreadAndFinishGame = testM("Link thread and finish game") {
    val card2 = DeckEntry(ExampleGameId, 25, ExampleId1, CardLocation.Table, Some(true))
    val card3 = DeckEntry(ExampleGameId, 32, ExampleId2, CardLocation.Table, Some(false))

    val initialGameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Spoofing))
    val initialDecks = Set(Card1, card2, card3)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer.copy(id = ExampleId1), ExamplePlayer, ExamplePlayer.copy(id = ExampleId2))),
      games = Set(ExampleGame)
    )
    val initialGameplayRepoState = GameplayRepoState(gamesState = Set(initialGameState), decks = initialDecks)

    for {
      gamesRepoState           <- Ref.make(initialGamesRepoState)
      gameplayRepoState        <- Ref.make(initialGameplayRepoState)
      sentEvents               <- Ref.make[List[InternalMessage]](List.empty)
      result                   <- ZIO
                                    .accessM[Matches](_.get.updateCardOnTableAs(ExampleGameId, ExamplePlayer.id, 1, UpdateTableDto))
                                    .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
      updatedGamesRepoState    <- gamesRepoState.get
      updatedGameplayRepoState <- gameplayRepoState.get
      events                   <- sentEvents.get
    } yield assert(result)(equalTo((): Unit)) &&
      assert(updatedGameplayRepoState.decks)(hasSameElements(Set(Card1.copy(threatLinked = Some(true)), card2, card3))) &&
      assert(events)(
        equalTo(
          List(
            InternalMessage.ThreatLinkedStatusChanged(ExampleGameId, 1, newStatus = true),
            InternalMessage.PlayerTakesTrick(ExampleGameId, Some(ExamplePlayer.id)),
            InternalMessage.GameFinished(ExampleGameId)
          )
        )
      ) &&
      assert(updatedGamesRepoState.tricksByPlayer)(hasSameElements(Set(FakeGameRepo.PlayerTricks(ExamplePlayer.id, ExampleGameId, 1)))) &&
      assert(updatedGamesRepoState.games)(hasSameElements(Set(ExampleGame.copy(finishedAt = Some(FirstRandomTime)))))
  }

  private val linkThreadAndGoToNextPlayer = testM("Link thread and continue with next player") {
    val card2 = DeckEntry(ExampleGameId, 25, ExampleId1, CardLocation.Hand, None)
    val card3 = DeckEntry(ExampleGameId, 32, ExampleId2, CardLocation.Hand, None)

    val initialGameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Spoofing))
    val initialDecks = Set(Card1, card2, card3)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer.copy(id = ExampleId1), ExamplePlayer, ExamplePlayer.copy(id = ExampleId2))),
      games = Set(ExampleGame)
    )
    val initialGameplayRepoState = GameplayRepoState(gamesState = Set(initialGameState), decks = initialDecks)

    for {
      gamesRepoState           <- Ref.make(initialGamesRepoState)
      gameplayRepoState        <- Ref.make(initialGameplayRepoState)
      sentEvents               <- Ref.make[List[InternalMessage]](List.empty)
      result                   <- ZIO
                                    .accessM[Matches](_.get.updateCardOnTableAs(ExampleGameId, ExamplePlayer.id, 1, UpdateTableDto))
                                    .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
      updatedGamesRepoState    <- gamesRepoState.get
      updatedGameplayRepoState <- gameplayRepoState.get
      events                   <- sentEvents.get
    } yield assert(result)(equalTo((): Unit)) &&
      assert(updatedGameplayRepoState.decks)(hasSameElements(Set(Card1.copy(threatLinked = Some(true)), card2, card3))) &&
      assert(events)(
        equalTo(
          List(
            InternalMessage.ThreatLinkedStatusChanged(ExampleGameId, 1, newStatus = true),
            InternalMessage.NextPlayer(ExampleGameId, ExampleId2)
          )
        )
      ) &&
      assert(updatedGamesRepoState.tricksByPlayer)(hasSameElements(Set.empty)) &&
      assert(updatedGamesRepoState.games)(hasSameElements(Set(ExampleGame))) &&
      assert(updatedGameplayRepoState.gamesState)(hasSameElements(Set(initialGameState.copy(currentPlayer = ExampleId2))))
  }

  private val linkThreadAndFinishTurn = testM("Link thread and finish turn") {
    val card2 = DeckEntry(ExampleGameId, 25, ExampleId1, CardLocation.Table, Some(true))
    val card3 = DeckEntry(ExampleGameId, 3, ExampleId2, CardLocation.Table, Some(true))
    val card4 = DeckEntry(ExampleGameId, 14, ExamplePlayer.id, CardLocation.Hand, None)
    val card5 = DeckEntry(ExampleGameId, 2, ExampleId1, CardLocation.Hand, None)
    val card6 = DeckEntry(ExampleGameId, 32, ExampleId2, CardLocation.Hand, None)

    val initialGameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Spoofing))
    val initialDecks = Set(Card1, card2, card3, card4, card5, card6)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer.copy(id = ExampleId1), ExamplePlayer, ExamplePlayer.copy(id = ExampleId2))),
      games = Set(ExampleGame)
    )
    val initialGameplayRepoState = GameplayRepoState(gamesState = Set(initialGameState), decks = initialDecks)

    for {
      gamesRepoState           <- Ref.make(initialGamesRepoState)
      gameplayRepoState        <- Ref.make(initialGameplayRepoState)
      sentEvents               <- Ref.make[List[InternalMessage]](List.empty)
      result                   <- ZIO
                                    .accessM[Matches](_.get.updateCardOnTableAs(ExampleGameId, ExamplePlayer.id, 1, UpdateTableDto))
                                    .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
      updatedGamesRepoState    <- gamesRepoState.get
      updatedGameplayRepoState <- gameplayRepoState.get
      events                   <- sentEvents.get
    } yield assert(result)(equalTo((): Unit)) &&
      assert(updatedGameplayRepoState.decks)(
        hasSameElements(
          Set(
            Card1.copy(threatLinked = Some(true), location = CardLocation.Out),
            card2.copy(location = CardLocation.Out),
            card3.copy(location = CardLocation.Out),
            card4,
            card5,
            card6
          )
        )
      ) &&
      assert(events)(
        equalTo(
          List(
            InternalMessage.ThreatLinkedStatusChanged(ExampleGameId, 1, newStatus = true),
            InternalMessage.PlayerTakesTrick(ExampleGameId, Some(ExampleId2)),
            InternalMessage.NextRound(ExampleGameId, ExampleId2)
          )
        )
      ) &&
      assert(updatedGamesRepoState.tricksByPlayer)(hasSameElements(Set(FakeGameRepo.PlayerTricks(ExampleId2, ExampleGameId, 1)))) &&
      assert(updatedGameplayRepoState.gamesState)(
        hasSameElements(Set(initialGameState.copy(currentPlayer = ExampleId2, leadingSuit = None)))
      )
  }

  private val linkThreadWhenGameNotFound = testM("Link thread when game not found") {
    val card2 = DeckEntry(ExampleGameId, 25, ExampleId1, CardLocation.Table, Some(true))
    val card3 = DeckEntry(ExampleGameId, 32, ExampleId2, CardLocation.Table, Some(false))

    val initialDecks = Set(Card1, card2, card3)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer.copy(id = ExampleId1), ExamplePlayer, ExamplePlayer.copy(id = ExampleId2))),
      games = Set(ExampleGame)
    )
    val initialGameplayRepoState = GameplayRepoState(gamesState = Set.empty, decks = initialDecks)

    for {
      gamesRepoState           <- Ref.make(initialGamesRepoState)
      gameplayRepoState        <- Ref.make(initialGameplayRepoState)
      sentEvents               <- Ref.make[List[InternalMessage]](List.empty)
      result                   <- ZIO
                                    .accessM[Matches](_.get.updateCardOnTableAs(ExampleGameId, ExamplePlayer.id, 1, UpdateTableDto))
                                    .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
                                    .either
      updatedGamesRepoState    <- gamesRepoState.get
      updatedGameplayRepoState <- gameplayRepoState.get
      events                   <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(GameNotFound(ExampleGameId)))) &&
      assert(updatedGamesRepoState)(equalTo(initialGamesRepoState)) &&
      assert(updatedGameplayRepoState)(equalTo(initialGameplayRepoState)) &&
      assert(events)(isEmpty)
  }

  private val linkThreadWhenNotAMember = testM("Link thread when user is not game member") {
    val card2 = DeckEntry(ExampleGameId, 25, ExampleId1, CardLocation.Table, Some(true))
    val card3 = DeckEntry(ExampleGameId, 32, ExampleId2, CardLocation.Table, Some(false))

    val initialGameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Spoofing))
    val initialDecks = Set(Card1, card2, card3)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer.copy(id = ExampleId1), ExamplePlayer.copy(id = ExampleId2))),
      games = Set(ExampleGame)
    )
    val initialGameplayRepoState = GameplayRepoState(gamesState = Set(initialGameState), decks = initialDecks)

    for {
      gamesRepoState           <- Ref.make(initialGamesRepoState)
      gameplayRepoState        <- Ref.make(initialGameplayRepoState)
      sentEvents               <- Ref.make[List[InternalMessage]](List.empty)
      result                   <- ZIO
                                    .accessM[Matches](_.get.updateCardOnTableAs(ExampleGameId, ExamplePlayer.id, 1, UpdateTableDto))
                                    .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
                                    .either
      updatedGamesRepoState    <- gamesRepoState.get
      updatedGameplayRepoState <- gameplayRepoState.get
      events                   <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(NotGameMember(ExampleGameId, ExamplePlayer.id)))) &&
      assert(updatedGamesRepoState)(equalTo(initialGamesRepoState)) &&
      assert(updatedGameplayRepoState)(equalTo(initialGameplayRepoState)) &&
      assert(events)(isEmpty)
  }

  private val linkThreadWhenNotPlayerRole = testM("Link thread when user has no Player role") {
    val card2 = DeckEntry(ExampleGameId, 25, ExampleId1, CardLocation.Table, Some(true))
    val card3 = DeckEntry(ExampleGameId, 32, ExampleId2, CardLocation.Table, Some(false))

    val initialGameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Spoofing))
    val initialDecks = Set(Card1, card2, card3)

    val initialGamesRepoState = GamesRepoState(
      players = Map(
        ExampleGameId -> List(
          ExamplePlayer.copy(id = ExampleId1),
          ExamplePlayer.copy(role = Some(PlayerRole.Observer)),
          ExamplePlayer.copy(id = ExampleId2)
        )
      ),
      games = Set(ExampleGame)
    )
    val initialGameplayRepoState = GameplayRepoState(gamesState = Set(initialGameState), decks = initialDecks)

    for {
      gamesRepoState           <- Ref.make(initialGamesRepoState)
      gameplayRepoState        <- Ref.make(initialGameplayRepoState)
      sentEvents               <- Ref.make[List[InternalMessage]](List.empty)
      result                   <- ZIO
                                    .accessM[Matches](_.get.updateCardOnTableAs(ExampleGameId, ExamplePlayer.id, 1, UpdateTableDto))
                                    .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
                                    .either
      updatedGamesRepoState    <- gamesRepoState.get
      updatedGameplayRepoState <- gameplayRepoState.get
      events                   <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(NotPlayer(ExampleGameId, ExamplePlayer.id)))) &&
      assert(updatedGamesRepoState)(equalTo(initialGamesRepoState)) &&
      assert(updatedGameplayRepoState)(equalTo(initialGameplayRepoState)) &&
      assert(events)(isEmpty)
  }

  private val linkThreadWhenOtherPlayersTurn = testM("Link thread when other players turn") {
    val card2 = DeckEntry(ExampleGameId, 25, ExampleId1, CardLocation.Table, Some(true))
    val card3 = DeckEntry(ExampleGameId, 32, ExampleId2, CardLocation.Table, Some(false))

    val initialGameState = FakeGameplayRepo.GameState(ExampleGameId, ExampleId2, Some(Suit.Spoofing))
    val initialDecks = Set(Card1, card2, card3)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer.copy(id = ExampleId1), ExamplePlayer, ExamplePlayer.copy(id = ExampleId2))),
      games = Set(ExampleGame)
    )
    val initialGameplayRepoState = GameplayRepoState(gamesState = Set(initialGameState), decks = initialDecks)

    for {
      gamesRepoState           <- Ref.make(initialGamesRepoState)
      gameplayRepoState        <- Ref.make(initialGameplayRepoState)
      sentEvents               <- Ref.make[List[InternalMessage]](List.empty)
      result                   <- ZIO
                                    .accessM[Matches](_.get.updateCardOnTableAs(ExampleGameId, ExamplePlayer.id, 1, UpdateTableDto))
                                    .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
                                    .either
      updatedGamesRepoState    <- gamesRepoState.get
      updatedGameplayRepoState <- gameplayRepoState.get
      events                   <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(OtherPlayersTurn(ExampleGameId, ExamplePlayer.id, ExampleId2)))) &&
      assert(updatedGamesRepoState)(equalTo(initialGamesRepoState)) &&
      assert(updatedGameplayRepoState)(equalTo(initialGameplayRepoState)) &&
      assert(events)(isEmpty)
  }

  private val linkThreadWhenCardDoesNotExistInDeck = testM("Link thread when card doesn't exist in deck") {
    val card2 = DeckEntry(ExampleGameId, 25, ExampleId1, CardLocation.Table, Some(true))
    val card3 = DeckEntry(ExampleGameId, 32, ExampleId2, CardLocation.Table, Some(false))

    val initialGameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Spoofing))
    val initialDecks = Set(Card1, card2, card3)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer.copy(id = ExampleId1), ExamplePlayer, ExamplePlayer.copy(id = ExampleId2))),
      games = Set(ExampleGame)
    )
    val initialGameplayRepoState = GameplayRepoState(gamesState = Set(initialGameState), decks = initialDecks)

    for {
      gamesRepoState           <- Ref.make(initialGamesRepoState)
      gameplayRepoState        <- Ref.make(initialGameplayRepoState)
      sentEvents               <- Ref.make[List[InternalMessage]](List.empty)
      result                   <- ZIO
                                    .accessM[Matches](_.get.updateCardOnTableAs(ExampleGameId, ExamplePlayer.id, 999, UpdateTableDto))
                                    .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
                                    .either
      updatedGamesRepoState    <- gamesRepoState.get
      updatedGameplayRepoState <- gameplayRepoState.get
      events                   <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(CardNotFound(ExampleGameId, 999)))) &&
      assert(updatedGamesRepoState)(equalTo(initialGamesRepoState)) &&
      assert(updatedGameplayRepoState)(equalTo(initialGameplayRepoState)) &&
      assert(events)(isEmpty)
  }

  private val linkThreadWhenCardsIsOwnedByAnotherPlayer = testM("Link thread when card is owned by another player") {
    val card2 = DeckEntry(ExampleGameId, 25, ExampleId1, CardLocation.Table, Some(true))
    val card3 = DeckEntry(ExampleGameId, 32, ExampleId2, CardLocation.Table, Some(false))

    val initialGameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Spoofing))
    val initialDecks = Set(Card1, card2, card3)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer.copy(id = ExampleId1), ExamplePlayer, ExamplePlayer.copy(id = ExampleId2))),
      games = Set(ExampleGame)
    )
    val initialGameplayRepoState = GameplayRepoState(gamesState = Set(initialGameState), decks = initialDecks)

    for {
      gamesRepoState           <- Ref.make(initialGamesRepoState)
      gameplayRepoState        <- Ref.make(initialGameplayRepoState)
      sentEvents               <- Ref.make[List[InternalMessage]](List.empty)
      result                   <- ZIO
                                    .accessM[Matches](_.get.updateCardOnTableAs(ExampleGameId, ExamplePlayer.id, 25, UpdateTableDto))
                                    .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
                                    .either
      updatedGamesRepoState    <- gamesRepoState.get
      updatedGameplayRepoState <- gameplayRepoState.get
      events                   <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(CardOwnedByAnotherUser(ExampleGameId, 25, ExamplePlayer.id, ExampleId1)))) &&
      assert(updatedGamesRepoState)(equalTo(initialGamesRepoState)) &&
      assert(updatedGameplayRepoState)(equalTo(initialGameplayRepoState)) &&
      assert(events)(isEmpty)
  }

  private val linkThreadWhenCardNotOnTheTable = testM("Link thread when card is not on the table") {
    val card2 = DeckEntry(ExampleGameId, 25, ExampleId1, CardLocation.Table, Some(true))
    val card3 = DeckEntry(ExampleGameId, 32, ExampleId2, CardLocation.Table, Some(false))
    val card4 = DeckEntry(ExampleGameId, 14, ExamplePlayer.id, CardLocation.Hand, None)

    val initialGameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Spoofing))
    val initialDecks = Set(Card1, card2, card3, card4)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer.copy(id = ExampleId1), ExamplePlayer, ExamplePlayer.copy(id = ExampleId2))),
      games = Set(ExampleGame)
    )
    val initialGameplayRepoState = GameplayRepoState(gamesState = Set(initialGameState), decks = initialDecks)

    for {
      gamesRepoState           <- Ref.make(initialGamesRepoState)
      gameplayRepoState        <- Ref.make(initialGameplayRepoState)
      sentEvents               <- Ref.make[List[InternalMessage]](List.empty)
      result                   <- ZIO
                                    .accessM[Matches](_.get.updateCardOnTableAs(ExampleGameId, ExamplePlayer.id, 14, UpdateTableDto))
                                    .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
                                    .either
      updatedGamesRepoState    <- gamesRepoState.get
      updatedGameplayRepoState <- gameplayRepoState.get
      events                   <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(UnexpectedCardLocation(ExampleGameId, 14, CardLocation.Table, CardLocation.Hand)))) &&
      assert(updatedGamesRepoState)(equalTo(initialGamesRepoState)) &&
      assert(updatedGameplayRepoState)(equalTo(initialGameplayRepoState)) &&
      assert(events)(isEmpty)
  }

  private val linkThreadWhenThreadAlreadyAssigned = testM("Link thread when thread already assigned") {
    val card2 = DeckEntry(ExampleGameId, 25, ExampleId1, CardLocation.Table, Some(true))
    val card3 = DeckEntry(ExampleGameId, 32, ExampleId2, CardLocation.Table, Some(false))

    val initialGameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Spoofing))
    val initialDecks = Set(Card1.copy(threatLinked = Some(false)), card2, card3)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer.copy(id = ExampleId1), ExamplePlayer, ExamplePlayer.copy(id = ExampleId2))),
      games = Set(ExampleGame)
    )
    val initialGameplayRepoState = GameplayRepoState(gamesState = Set(initialGameState), decks = initialDecks)

    for {
      gamesRepoState           <- Ref.make(initialGamesRepoState)
      gameplayRepoState        <- Ref.make(initialGameplayRepoState)
      sentEvents               <- Ref.make[List[InternalMessage]](List.empty)
      result                   <- ZIO
                                    .accessM[Matches](_.get.updateCardOnTableAs(ExampleGameId, ExamplePlayer.id, 1, UpdateTableDto))
                                    .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
                                    .either
      updatedGamesRepoState    <- gamesRepoState.get
      updatedGameplayRepoState <- gameplayRepoState.get
      events                   <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(ThreatStatusAlreadyAssigned(ExampleGameId, 1, false)))) &&
      assert(updatedGamesRepoState)(equalTo(initialGamesRepoState)) &&
      assert(updatedGameplayRepoState)(equalTo(initialGameplayRepoState)) &&
      assert(events)(isEmpty)
  }

  private val linkThreadSelectPlayerForward = testM("Link thread and select next player after current player") {
    val card5 = DeckEntry(ExampleGameId, 2, ExampleId1, CardLocation.Hand, None)
    val card6 = DeckEntry(ExampleGameId, 32, ExampleId2, CardLocation.Hand, None)

    val initialGameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Spoofing))
    val initialDecks = Set(Card1, card5, card6)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer.copy(id = ExampleId1), ExamplePlayer, ExamplePlayer.copy(id = ExampleId2))),
      games = Set(ExampleGame)
    )
    val initialGameplayRepoState = GameplayRepoState(gamesState = Set(initialGameState), decks = initialDecks)

    for {
      gamesRepoState           <- Ref.make(initialGamesRepoState)
      gameplayRepoState        <- Ref.make(initialGameplayRepoState)
      sentEvents               <- Ref.make[List[InternalMessage]](List.empty)
      _                        <- ZIO
                                    .accessM[Matches](_.get.updateCardOnTableAs(ExampleGameId, ExamplePlayer.id, 1, UpdateTableDto))
                                    .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
      updatedGameplayRepoState <- gameplayRepoState.get
    } yield assert(updatedGameplayRepoState.gamesState)(
      hasSameElements(Set(initialGameState.copy(currentPlayer = ExampleId2)))
    )
  }

  private val linkThreadSelectPlayerBackward = testM("Link thread and select next player after current player") {
    val card5 = DeckEntry(ExampleGameId, 2, ExampleId1, CardLocation.Hand, None)
    val card6 = DeckEntry(ExampleGameId, 32, ExampleId2, CardLocation.Out, None)

    val initialGameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Spoofing))
    val initialDecks = Set(Card1, card5, card6)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer.copy(id = ExampleId1), ExamplePlayer, ExamplePlayer.copy(id = ExampleId2))),
      games = Set(ExampleGame)
    )
    val initialGameplayRepoState = GameplayRepoState(gamesState = Set(initialGameState), decks = initialDecks)

    for {
      gamesRepoState           <- Ref.make(initialGamesRepoState)
      gameplayRepoState        <- Ref.make(initialGameplayRepoState)
      sentEvents               <- Ref.make[List[InternalMessage]](List.empty)
      _                        <- ZIO
                                    .accessM[Matches](_.get.updateCardOnTableAs(ExampleGameId, ExamplePlayer.id, 1, UpdateTableDto))
                                    .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
      updatedGameplayRepoState <- gameplayRepoState.get
    } yield assert(updatedGameplayRepoState.gamesState)(
      hasSameElements(Set(initialGameState.copy(currentPlayer = ExampleId1)))
    )
  }

  private val linkThreadAndRecordTrick = testM("Link thread and record trick for winner") {
    val card2 = DeckEntry(ExampleGameId, 25, ExampleId1, CardLocation.Table, Some(true))
    val card3 = DeckEntry(ExampleGameId, 3, ExampleId2, CardLocation.Table, Some(true))
    val card4 = DeckEntry(ExampleGameId, 14, ExamplePlayer.id, CardLocation.Hand, None)
    val card5 = DeckEntry(ExampleGameId, 2, ExampleId1, CardLocation.Hand, None)
    val card6 = DeckEntry(ExampleGameId, 32, ExampleId2, CardLocation.Hand, None)

    val initialGameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Spoofing))
    val initialDecks = Set(Card1, card2, card3, card4, card5, card6)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer.copy(id = ExampleId1), ExamplePlayer, ExamplePlayer.copy(id = ExampleId2))),
      games = Set(ExampleGame)
    )
    val initialGameplayRepoState = GameplayRepoState(gamesState = Set(initialGameState), decks = initialDecks)

    for {
      gamesRepoState        <- Ref.make(initialGamesRepoState)
      gameplayRepoState     <- Ref.make(initialGameplayRepoState)
      sentEvents            <- Ref.make[List[InternalMessage]](List.empty)
      _                     <- ZIO
                                 .accessM[Matches](_.get.updateCardOnTableAs(ExampleGameId, ExamplePlayer.id, 1, UpdateTableDto))
                                 .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
      updatedGamesRepoState <- gamesRepoState.get
    } yield assert(updatedGamesRepoState.tricksByPlayer)(hasSameElements(Set(FakeGameRepo.PlayerTricks(ExampleId2, ExampleGameId, 1))))
  }

  private val notTakeTheTrickWhenThreatNotLinked = testM("Do not take the trick when thread is not linked") {
    val card2 = DeckEntry(ExampleGameId, 25, ExampleId1, CardLocation.Table, Some(true))
    val card3 = DeckEntry(ExampleGameId, 3, ExampleId2, CardLocation.Table, Some(false))
    val card4 = DeckEntry(ExampleGameId, 14, ExamplePlayer.id, CardLocation.Hand, None)
    val card5 = DeckEntry(ExampleGameId, 2, ExampleId1, CardLocation.Hand, None)
    val card6 = DeckEntry(ExampleGameId, 32, ExampleId2, CardLocation.Hand, None)

    val initialGameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Spoofing))
    val initialDecks = Set(Card1, card2, card3, card4, card5, card6)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer.copy(id = ExampleId1), ExamplePlayer, ExamplePlayer.copy(id = ExampleId2))),
      games = Set(ExampleGame)
    )
    val initialGameplayRepoState = GameplayRepoState(gamesState = Set(initialGameState), decks = initialDecks)

    for {
      gamesRepoState        <- Ref.make(initialGamesRepoState)
      gameplayRepoState     <- Ref.make(initialGameplayRepoState)
      sentEvents            <- Ref.make[List[InternalMessage]](List.empty)
      _                     <- ZIO
                                 .accessM[Matches](_.get.updateCardOnTableAs(ExampleGameId, ExamplePlayer.id, 1, UpdateTableDto))
                                 .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
      updatedGamesRepoState <- gamesRepoState.get
    } yield assert(updatedGamesRepoState.tricksByPlayer)(
      hasSameElements(Set(FakeGameRepo.PlayerTricks(ExamplePlayer.id, ExampleGameId, 1)))
    )
  }

  private val takeTheTrickWhenTrumpCardPlayed = testM("Take the trick when trump card played") {
    val card2 = DeckEntry(ExampleGameId, 70, ExampleId1, CardLocation.Table, Some(true))
    val card3 = DeckEntry(ExampleGameId, 3, ExampleId2, CardLocation.Table, Some(true))
    val card4 = DeckEntry(ExampleGameId, 14, ExamplePlayer.id, CardLocation.Hand, None)
    val card5 = DeckEntry(ExampleGameId, 2, ExampleId1, CardLocation.Hand, None)
    val card6 = DeckEntry(ExampleGameId, 32, ExampleId2, CardLocation.Hand, None)

    val initialGameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Spoofing))
    val initialDecks = Set(Card1, card2, card3, card4, card5, card6)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer.copy(id = ExampleId1), ExamplePlayer, ExamplePlayer.copy(id = ExampleId2))),
      games = Set(ExampleGame)
    )
    val initialGameplayRepoState = GameplayRepoState(gamesState = Set(initialGameState), decks = initialDecks)

    for {
      gamesRepoState        <- Ref.make(initialGamesRepoState)
      gameplayRepoState     <- Ref.make(initialGameplayRepoState)
      sentEvents            <- Ref.make[List[InternalMessage]](List.empty)
      _                     <- ZIO
                                 .accessM[Matches](_.get.updateCardOnTableAs(ExampleGameId, ExamplePlayer.id, 1, UpdateTableDto))
                                 .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
      updatedGamesRepoState <- gamesRepoState.get
    } yield assert(updatedGamesRepoState.tricksByPlayer)(hasSameElements(Set(FakeGameRepo.PlayerTricks(ExampleId1, ExampleGameId, 1))))
  }

  private val doNotRecordTheTrickWhenNobodyWins = testM("Do not record the trick when nobody wins") {
    val card2 = DeckEntry(ExampleGameId, 25, ExampleId1, CardLocation.Table, None)
    val card3 = DeckEntry(ExampleGameId, 3, ExampleId2, CardLocation.Table, None)
    val card4 = DeckEntry(ExampleGameId, 14, ExamplePlayer.id, CardLocation.Hand, None)
    val card5 = DeckEntry(ExampleGameId, 2, ExampleId1, CardLocation.Hand, None)
    val card6 = DeckEntry(ExampleGameId, 32, ExampleId2, CardLocation.Hand, None)

    val initialGameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Spoofing))
    val initialDecks = Set(Card1, card2, card3, card4, card5, card6)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer.copy(id = ExampleId1), ExamplePlayer, ExamplePlayer.copy(id = ExampleId2))),
      games = Set(ExampleGame)
    )
    val initialGameplayRepoState = GameplayRepoState(gamesState = Set(initialGameState), decks = initialDecks)

    for {
      gamesRepoState        <- Ref.make(initialGamesRepoState)
      gameplayRepoState     <- Ref.make(initialGameplayRepoState)
      sentEvents            <- Ref.make[List[InternalMessage]](List.empty)
      _                     <- ZIO
                                 .accessM[Matches](_.get.updateCardOnTableAs(ExampleGameId, ExamplePlayer.id, 1, TableCardReqDto(false)))
                                 .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
      updatedGamesRepoState <- gamesRepoState.get
    } yield assert(updatedGamesRepoState.tricksByPlayer)(isEmpty)
  }

  private val selectWinnerForNextTurnInitPlayer = testM("Select winner for next turn init player") {
    val card2 = DeckEntry(ExampleGameId, 25, ExampleId1, CardLocation.Table, Some(true))
    val card3 = DeckEntry(ExampleGameId, 3, ExampleId2, CardLocation.Table, Some(true))
    val card4 = DeckEntry(ExampleGameId, 14, ExamplePlayer.id, CardLocation.Hand, None)
    val card5 = DeckEntry(ExampleGameId, 2, ExampleId1, CardLocation.Hand, None)
    val card6 = DeckEntry(ExampleGameId, 32, ExampleId2, CardLocation.Hand, None)

    val initialGameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Spoofing))
    val initialDecks = Set(Card1, card2, card3, card4, card5, card6)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer.copy(id = ExampleId1), ExamplePlayer, ExamplePlayer.copy(id = ExampleId2))),
      games = Set(ExampleGame)
    )
    val initialGameplayRepoState = GameplayRepoState(gamesState = Set(initialGameState), decks = initialDecks)

    for {
      gamesRepoState           <- Ref.make(initialGamesRepoState)
      gameplayRepoState        <- Ref.make(initialGameplayRepoState)
      sentEvents               <- Ref.make[List[InternalMessage]](List.empty)
      _                        <- ZIO
                                    .accessM[Matches](_.get.updateCardOnTableAs(ExampleGameId, ExamplePlayer.id, 1, UpdateTableDto))
                                    .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
      updatedGameplayRepoState <- gameplayRepoState.get
    } yield assert(updatedGameplayRepoState.gamesState)(
      hasSameElements(Set(initialGameState.copy(currentPlayer = ExampleId2, leadingSuit = None)))
    )
  }

  private val selectSecondWinnerForNextTurnInitPlayerIfWinnerHasNoCardsOnHand =
    testM("select second winner for next turn init player if winner has no cards on hand") {
      val card2 = DeckEntry(ExampleGameId, 25, ExampleId1, CardLocation.Table, Some(true))
      val card3 = DeckEntry(ExampleGameId, 3, ExampleId2, CardLocation.Table, Some(true))
      val card4 = DeckEntry(ExampleGameId, 14, ExamplePlayer.id, CardLocation.Hand, None)
      val card5 = DeckEntry(ExampleGameId, 2, ExampleId1, CardLocation.Hand, None)

      val initialGameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Spoofing))
      val initialDecks = Set(Card1, card2, card3, card4, card5)

      val initialGamesRepoState = GamesRepoState(
        players = Map(ExampleGameId -> List(ExamplePlayer.copy(id = ExampleId1), ExamplePlayer, ExamplePlayer.copy(id = ExampleId2))),
        games = Set(ExampleGame)
      )
      val initialGameplayRepoState = GameplayRepoState(gamesState = Set(initialGameState), decks = initialDecks)

      for {
        gamesRepoState           <- Ref.make(initialGamesRepoState)
        gameplayRepoState        <- Ref.make(initialGameplayRepoState)
        sentEvents               <- Ref.make[List[InternalMessage]](List.empty)
        _                        <- ZIO
                                      .accessM[Matches](_.get.updateCardOnTableAs(ExampleGameId, ExamplePlayer.id, 1, UpdateTableDto))
                                      .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
        updatedGameplayRepoState <- gameplayRepoState.get
      } yield assert(updatedGameplayRepoState.gamesState)(
        hasSameElements(Set(initialGameState.copy(currentPlayer = ExamplePlayer.id, leadingSuit = None)))
      )
    }

  private val selectRandomNextTurnInitPlayerIfNoWinner = testM("Select random next turn init player if no winner") {
    val card2 = DeckEntry(ExampleGameId, 25, ExampleId1, CardLocation.Table, Some(false))
    val card3 = DeckEntry(ExampleGameId, 3, ExampleId2, CardLocation.Table, Some(false))
    val card4 = DeckEntry(ExampleGameId, 14, ExamplePlayer.id, CardLocation.Hand, None)
    val card5 = DeckEntry(ExampleGameId, 70, ExampleId1, CardLocation.Hand, None)
    val card6 = DeckEntry(ExampleGameId, 32, ExampleId2, CardLocation.Hand, None)

    val initialGameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Spoofing))
    val initialDecks = Set(Card1, card2, card3, card4, card5, card6)

    val initialGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer.copy(id = ExampleId1), ExamplePlayer, ExamplePlayer.copy(id = ExampleId2))),
      games = Set(ExampleGame)
    )
    val initialGameplayRepoState = GameplayRepoState(gamesState = Set(initialGameState), decks = initialDecks)

    for {
      gamesRepoState           <- Ref.make(initialGamesRepoState)
      gameplayRepoState        <- Ref.make(initialGameplayRepoState)
      sentEvents               <- Ref.make[List[InternalMessage]](List.empty)
      _                        <- ZIO
                                    .accessM[Matches](_.get.updateCardOnTableAs(ExampleGameId, ExamplePlayer.id, 1, TableCardReqDto(false)))
                                    .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
      updatedGameplayRepoState <- gameplayRepoState.get
    } yield assert(updatedGameplayRepoState.gamesState)(
      hasSameElements(Set(initialGameState.copy(currentPlayer = ExampleId1, leadingSuit = None)))
    )
  }

  private val playTheCardWithSameSuit = testM("Play the card with matching suit") {
    val gameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Tampering))
    val card1 = Card1.copy(location = CardLocation.Hand)

    val initGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer))
    )
    val initGameplayRepoState = GameplayRepoState(
      gamesState = Set(gameState),
      decks = Set(Card4, card1)
    )

    for {
      gamesRepoState     <- Ref.make(initGamesRepoState)
      gameplayRepoState  <- Ref.make(initGameplayRepoState)
      sentEvents         <- Ref.make[List[InternalMessage]](List.empty)
      result             <- ZIO
                              .accessM[Matches](_.get.putCardOnTheTableAs(ExampleGameId, ExamplePlayer.id, 25))
                              .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
      updateGameplayRepo <- gameplayRepoState.get
      events             <- sentEvents.get
    } yield assert(result)(equalTo((): Unit)) &&
      assert(updateGameplayRepo.decks)(hasSameElements(Set(Card4.copy(location = CardLocation.Table), card1))) &&
      assert(updateGameplayRepo.gamesState)(hasSameElements(initGameplayRepoState.gamesState)) &&
      assert(events)(hasSameElements(List(InternalMessage.CardPlayed(ExampleGameId, ExamplePlayer.id, 25))))
  }

  private val playTheCardWhenNoMatchingCardOnHand = testM("Play the card when no matching card on hand") {
    val gameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Spoofing))

    val initGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer))
    )
    val initGameplayRepoState = GameplayRepoState(
      gamesState = Set(gameState),
      decks = Set(Card4)
    )

    for {
      gamesRepoState     <- Ref.make(initGamesRepoState)
      gameplayRepoState  <- Ref.make(initGameplayRepoState)
      sentEvents         <- Ref.make[List[InternalMessage]](List.empty)
      result             <- ZIO
                              .accessM[Matches](_.get.putCardOnTheTableAs(ExampleGameId, ExamplePlayer.id, 25))
                              .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
      updateGameplayRepo <- gameplayRepoState.get
      events             <- sentEvents.get
    } yield assert(result)(equalTo((): Unit)) &&
      assert(updateGameplayRepo.decks)(hasSameElements(Set(Card4.copy(location = CardLocation.Table)))) &&
      assert(updateGameplayRepo.gamesState)(hasSameElements(initGameplayRepoState.gamesState)) &&
      assert(events)(hasSameElements(List(InternalMessage.CardPlayed(ExampleGameId, ExamplePlayer.id, 25))))
  }

  private val playTheCardWhenNoLeadingSuit = testM("Play the card when leading suit was not defined") {
    val gameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, None)
    val card1 = Card1.copy(location = CardLocation.Hand)

    val initGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer))
    )
    val initGameplayRepoState = GameplayRepoState(
      gamesState = Set(gameState),
      decks = Set(Card4, card1)
    )

    for {
      gamesRepoState     <- Ref.make(initGamesRepoState)
      gameplayRepoState  <- Ref.make(initGameplayRepoState)
      sentEvents         <- Ref.make[List[InternalMessage]](List.empty)
      result             <- ZIO
                              .accessM[Matches](_.get.putCardOnTheTableAs(ExampleGameId, ExamplePlayer.id, 25))
                              .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
      updateGameplayRepo <- gameplayRepoState.get
      events             <- sentEvents.get
    } yield assert(result)(equalTo((): Unit)) &&
      assert(updateGameplayRepo.decks)(hasSameElements(Set(Card4.copy(location = CardLocation.Table), card1))) &&
      assert(updateGameplayRepo.gamesState)(hasSameElements(Set(gameState.copy(leadingSuit = Some(Suit.Tampering))))) &&
      assert(events)(hasSameElements(List(InternalMessage.CardPlayed(ExampleGameId, ExamplePlayer.id, 25))))
  }

  private val playTheCardWithNotMatchingSuit = testM("Play the card with not matching suit") {
    val gameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Spoofing))
    val card1 = Card1.copy(location = CardLocation.Hand)

    val initGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer))
    )
    val initGameplayRepoState = GameplayRepoState(
      gamesState = Set(gameState),
      decks = Set(Card4, card1)
    )

    for {
      gamesRepoState     <- Ref.make(initGamesRepoState)
      gameplayRepoState  <- Ref.make(initGameplayRepoState)
      sentEvents         <- Ref.make[List[InternalMessage]](List.empty)
      result             <- ZIO
                              .accessM[Matches](_.get.putCardOnTheTableAs(ExampleGameId, ExamplePlayer.id, 25))
                              .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
                              .either
      updateGameplayRepo <- gameplayRepoState.get
      events             <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(SuitDoesNotMatch(ExampleGameId, Suit.Tampering, Suit.Spoofing)))) &&
      assert(updateGameplayRepo)(equalTo(initGameplayRepoState)) &&
      assert(events)(isEmpty)
  }

  private val playTheCardWhenGameNotFound = testM("Play the card when game not found") {
    val card1 = Card1.copy(location = CardLocation.Hand)

    val initGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer))
    )
    val initGameplayRepoState = GameplayRepoState(
      gamesState = Set.empty,
      decks = Set(Card4, card1)
    )

    for {
      gamesRepoState     <- Ref.make(initGamesRepoState)
      gameplayRepoState  <- Ref.make(initGameplayRepoState)
      sentEvents         <- Ref.make[List[InternalMessage]](List.empty)
      result             <- ZIO
                              .accessM[Matches](_.get.putCardOnTheTableAs(ExampleGameId, ExamplePlayer.id, 25))
                              .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
                              .either
      updateGameplayRepo <- gameplayRepoState.get
      events             <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(GameNotFound(ExampleGameId)))) &&
      assert(updateGameplayRepo)(equalTo(initGameplayRepoState)) &&
      assert(events)(isEmpty)
  }

  private val playTheCardWhenUserIsNotGameMember = testM("Play the card when user is not game member") {
    val gameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Tampering))
    val card1 = Card1.copy(location = CardLocation.Hand)

    val initGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List.empty)
    )
    val initGameplayRepoState = GameplayRepoState(
      gamesState = Set(gameState),
      decks = Set(Card4, card1)
    )

    for {
      gamesRepoState     <- Ref.make(initGamesRepoState)
      gameplayRepoState  <- Ref.make(initGameplayRepoState)
      sentEvents         <- Ref.make[List[InternalMessage]](List.empty)
      result             <- ZIO
                              .accessM[Matches](_.get.putCardOnTheTableAs(ExampleGameId, ExamplePlayer.id, 25))
                              .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
                              .either
      updateGameplayRepo <- gameplayRepoState.get
      events             <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(NotGameMember(ExampleGameId, ExamplePlayer.id)))) &&
      assert(updateGameplayRepo)(equalTo(initGameplayRepoState)) &&
      assert(events)(isEmpty)
  }

  private val playTheCardWhenUserIsNotPlayer = testM("Play the card when user is not player") {
    val gameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Tampering))
    val card1 = Card1.copy(location = CardLocation.Hand)

    val initGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer.copy(role = Some(PlayerRole.Observer))))
    )
    val initGameplayRepoState = GameplayRepoState(
      gamesState = Set(gameState),
      decks = Set(Card4, card1)
    )

    for {
      gamesRepoState     <- Ref.make(initGamesRepoState)
      gameplayRepoState  <- Ref.make(initGameplayRepoState)
      sentEvents         <- Ref.make[List[InternalMessage]](List.empty)
      result             <- ZIO
                              .accessM[Matches](_.get.putCardOnTheTableAs(ExampleGameId, ExamplePlayer.id, 25))
                              .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
                              .either
      updateGameplayRepo <- gameplayRepoState.get
      events             <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(NotPlayer(ExampleGameId, ExamplePlayer.id)))) &&
      assert(updateGameplayRepo)(equalTo(initGameplayRepoState)) &&
      assert(events)(isEmpty)
  }

  private val playTheCardWhenCardNotFound = testM("Play the card when card not found") {
    val gameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Tampering))
    val card1 = Card1.copy(location = CardLocation.Hand)

    val initGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer))
    )
    val initGameplayRepoState = GameplayRepoState(
      gamesState = Set(gameState),
      decks = Set(Card4, card1)
    )

    for {
      gamesRepoState     <- Ref.make(initGamesRepoState)
      gameplayRepoState  <- Ref.make(initGameplayRepoState)
      sentEvents         <- Ref.make[List[InternalMessage]](List.empty)
      result             <- ZIO
                              .accessM[Matches](_.get.putCardOnTheTableAs(ExampleGameId, ExamplePlayer.id, 999))
                              .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
                              .either
      updateGameplayRepo <- gameplayRepoState.get
      events             <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(CardNotFound(ExampleGameId, 999)))) &&
      assert(updateGameplayRepo)(equalTo(initGameplayRepoState)) &&
      assert(events)(isEmpty)
  }

  private val playTheCardWhenOtherPlayersTurn = testM("Play the card when other players turn") {
    val gameState = FakeGameplayRepo.GameState(ExampleGameId, ExampleId1, Some(Suit.Tampering))
    val card1 = Card1.copy(location = CardLocation.Hand)

    val initGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer))
    )
    val initGameplayRepoState = GameplayRepoState(
      gamesState = Set(gameState),
      decks = Set(Card4, card1)
    )

    for {
      gamesRepoState     <- Ref.make(initGamesRepoState)
      gameplayRepoState  <- Ref.make(initGameplayRepoState)
      sentEvents         <- Ref.make[List[InternalMessage]](List.empty)
      result             <- ZIO
                              .accessM[Matches](_.get.putCardOnTheTableAs(ExampleGameId, ExamplePlayer.id, 25))
                              .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
                              .either
      updateGameplayRepo <- gameplayRepoState.get
      events             <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(OtherPlayersTurn(ExampleGameId, ExamplePlayer.id, ExampleId1)))) &&
      assert(updateGameplayRepo)(equalTo(initGameplayRepoState)) &&
      assert(events)(isEmpty)
  }

  private val playTheCardWhenCardOwnedByAnotherUser = testM("Play the card when card is owned by another user") {
    val gameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Tampering))
    val card1 = Card1.copy(location = CardLocation.Hand)
    val card3 = Card3.copy(location = CardLocation.Hand)

    val initGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer))
    )
    val initGameplayRepoState = GameplayRepoState(
      gamesState = Set(gameState),
      decks = Set(Card4, card1, card3)
    )

    for {
      gamesRepoState     <- Ref.make(initGamesRepoState)
      gameplayRepoState  <- Ref.make(initGameplayRepoState)
      sentEvents         <- Ref.make[List[InternalMessage]](List.empty)
      result             <- ZIO
                              .accessM[Matches](_.get.putCardOnTheTableAs(ExampleGameId, ExamplePlayer.id, 3))
                              .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
                              .either
      updateGameplayRepo <- gameplayRepoState.get
      events             <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(CardOwnedByAnotherUser(ExampleGameId, 3, ExamplePlayer.id, ExampleId1)))) &&
      assert(updateGameplayRepo)(equalTo(initGameplayRepoState)) &&
      assert(events)(isEmpty)
  }

  private val playTheCardWhenCardIsNotOnTheHand = testM("Play the card when card is not on the hand") {
    val gameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Tampering))
    val card1 = Card1.copy(location = CardLocation.Hand)
    val card4 = Card4.copy(location = CardLocation.Out)

    val initGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer))
    )
    val initGameplayRepoState = GameplayRepoState(
      gamesState = Set(gameState),
      decks = Set(card4, card1)
    )

    for {
      gamesRepoState     <- Ref.make(initGamesRepoState)
      gameplayRepoState  <- Ref.make(initGameplayRepoState)
      sentEvents         <- Ref.make[List[InternalMessage]](List.empty)
      result             <- ZIO
                              .accessM[Matches](_.get.putCardOnTheTableAs(ExampleGameId, ExamplePlayer.id, 25))
                              .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
                              .either
      updateGameplayRepo <- gameplayRepoState.get
      events             <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(UnexpectedCardLocation(ExampleGameId, 25, CardLocation.Hand, CardLocation.Out)))) &&
      assert(updateGameplayRepo)(equalTo(initGameplayRepoState)) &&
      assert(events)(isEmpty)
  }

  private val playTheCardWhenPlayerAlreadyPlayedAnotherCard = testM("Play the card when player already played another card") {
    val gameState = FakeGameplayRepo.GameState(ExampleGameId, ExamplePlayer.id, Some(Suit.Tampering))
    val card1 = Card1.copy(location = CardLocation.Hand)
    val card3 = Card3.copy(playerId = ExamplePlayer.id, location = CardLocation.Table)

    val initGamesRepoState = GamesRepoState(
      players = Map(ExampleGameId -> List(ExamplePlayer))
    )
    val initGameplayRepoState = GameplayRepoState(
      gamesState = Set(gameState),
      decks = Set(Card4, card1, card3)
    )

    for {
      gamesRepoState     <- Ref.make(initGamesRepoState)
      gameplayRepoState  <- Ref.make(initGameplayRepoState)
      sentEvents         <- Ref.make[List[InternalMessage]](List.empty)
      result             <- ZIO
                              .accessM[Matches](_.get.putCardOnTheTableAs(ExampleGameId, ExamplePlayer.id, 25))
                              .provideCustomLayer(createLayer(gamesRepoState, gameplayRepoState, sentEvents))
                              .either
      updateGameplayRepo <- gameplayRepoState.get
      events             <- sentEvents.get
    } yield assert(result)(isLeft(equalTo(PlayerAlreadyPlayedCard(ExampleGameId, ExamplePlayer.id, 3, 25)))) &&
      assert(updateGameplayRepo)(equalTo(initGameplayRepoState)) &&
      assert(events)(isEmpty)
  }

}
