import { Round } from "./../types/Round"
import { CardPlayed } from "./../types/Events"
import {
    cardPlayedAction,
    fetchMatchStateAction,
    fetchScoresAction,
    nextPlayerAction,
    nextTurnAction,
    playerTakesTrickAction,
    threatStatusAssignedAction,
} from "./Actions"
import { round, usersCard } from "../../../testutils/constants/match"
import { OperationStatus } from "./../../../application/store/async/AsyncOperationResult"
import { matchReducer } from "./Reducers"
import { scores } from "../../../testutils/constants/game"

const defaultState = {} as ReturnType<typeof matchReducer>

describe("Match reducers", () => {
    describe("Match state", () => {
        const roundData = { status: OperationStatus.FINISHED, params: "foo-bar", data: round }
        describe("On threat status assigned", () => {
            const payload = { gameId: "foo-bar", cardNumber: 39, newStatus: false, playerId: "111" }

            it("should update table", () => {
                const state = { ...defaultState, matchState: roundData }
                const action = threatStatusAssignedAction(payload)
                const result = matchReducer(state, action)
                expect(result.matchState.data).toStrictEqual({
                    ...round,
                    table: [{ ...usersCard, threatLinked: false }],
                })
            })

            it("should not update table if game has different id", () => {
                const state = { ...defaultState, matchState: roundData }
                const action = threatStatusAssignedAction({ ...payload, gameId: "other-game" })
                const result = matchReducer(state, action)
                expect(result.matchState.data).toStrictEqual(round)
            })

            it("should not update table if card is not on the table", () => {
                const state = { ...defaultState, matchState: roundData }
                const action = threatStatusAssignedAction({ ...payload, cardNumber: 99 })
                const result = matchReducer(state, action)
                expect(result.matchState.data).toStrictEqual(round)
            })
        })

        describe("On next player", () => {
            const payload = { gameId: "foo-bar", newPlayer: "222" }

            it("should update current player", () => {
                const state = { ...defaultState, matchState: roundData }
                const action = nextPlayerAction(payload)
                const result = matchReducer(state, action)
                expect(result.matchState.data?.state).toStrictEqual({ ...round.state, currentPlayer: "222" })
            })

            it("should not update current player if current game has different id", () => {
                const state = { ...defaultState, matchState: roundData }
                const action = nextPlayerAction({ ...payload, gameId: "other-game" })
                const result = matchReducer(state, action)
                expect(result.matchState.data?.state).toStrictEqual(round.state)
            })
        })

        describe("On card played", () => {
            const payload: CardPlayed = {
                gameId: "foo-bar",
                playerId: "111",
                card: {
                    cardNumber: 29,
                    value: "Five",
                    suit: "Repudiation",
                    text: "Lorem ipsum",
                    example: "Lorem ipsum",
                    mitigation: "Lorem ipsum",
                },
                location: "Table",
                threatLinked: null,
            }

            describe("Table", () => {
                it("should be updated", () => {
                    const state = { ...defaultState, matchState: roundData }
                    const action = cardPlayedAction(payload)
                    const result = matchReducer(state, action)
                    expect(result.matchState.data?.table).toStrictEqual(round.table.concat(payload))
                })

                it("should not be updated if current game has different id", () => {
                    const state = { ...defaultState, matchState: roundData }
                    const action = cardPlayedAction({ ...payload, gameId: "other-game" })
                    const result = matchReducer(state, action)
                    expect(result.matchState.data?.table).toStrictEqual(round.table)
                })
            })

            describe("Hand", () => {
                it("should be updated", () => {
                    const initHand = round.hand.concat(payload.card)
                    const initRound = { ...round, hand: initHand }
                    const state = { ...defaultState, matchState: { ...roundData, data: initRound } }
                    const action = cardPlayedAction(payload)
                    const result = matchReducer(state, action)
                    expect(result.matchState.data?.hand).toEqual(round.hand)
                })

                it("should not be updated if player did not have this card before", () => {
                    const secondCard = { ...payload.card, cardNumber: 73 }
                    const initHand = round.hand.concat(secondCard)
                    const initRound = { ...round, hand: initHand }
                    const state = { ...defaultState, matchState: { ...roundData, data: initRound } }
                    const action = cardPlayedAction(payload)
                    const result = matchReducer(state, action)
                    expect(result.matchState.data?.hand).toEqual(round.hand.concat(secondCard))
                })

                it("should not be updated be updated if game has different id", () => {
                    const initHand = round.hand.concat(payload.card)
                    const initRound = { ...round, hand: initHand }
                    const state = { ...defaultState, matchState: { ...roundData, data: initRound } }
                    const action = cardPlayedAction({ ...payload, gameId: "other-game" })
                    const result = matchReducer(state, action)
                    expect(result.matchState.data?.hand).toEqual(round.hand.concat(payload.card))
                })
            })

            describe("Leading suit", () => {
                it("should be set", () => {
                    const initRound = { ...round, state: { ...round.state, leadingSuit: null } }
                    const state = { ...defaultState, matchState: { ...roundData, data: initRound } }
                    const action = cardPlayedAction(payload)
                    const result = matchReducer(state, action)
                    expect(result.matchState.data?.state.leadingSuit).toEqual(payload.card.suit)
                })

                it("should not be set if already exists", () => {
                    const initRound: Round = { ...round, state: { ...round.state, leadingSuit: "DenialOfService" } }
                    const state = { ...defaultState, matchState: { ...roundData, data: initRound } }
                    const action = cardPlayedAction(payload)
                    const result = matchReducer(state, action)
                    expect(result.matchState.data?.state.leadingSuit).toEqual("DenialOfService")
                })

                it("should be set if game has different id", () => {
                    const initRound = { ...round, state: { ...round.state, leadingSuit: null } }
                    const state = { ...defaultState, matchState: { ...roundData, data: initRound } }
                    const action = cardPlayedAction({ ...payload, gameId: "other-game" })
                    const result = matchReducer(state, action)
                    expect(result.matchState.data?.state.leadingSuit).toBe(null)
                })
            })
        })

        describe("On next turn", () => {
            const payload = { gameId: "foo-bar", player: "333" }

            describe("Table", () => {
                it("should be cleared", () => {
                    const state = { ...defaultState, matchState: roundData }
                    const action = nextTurnAction(payload)
                    const result = matchReducer(state, action)
                    expect(result.matchState.data?.table).toHaveLength(0)
                })

                it("should not be cleared if game has different id", () => {
                    const state = { ...defaultState, matchState: roundData }
                    const action = nextTurnAction({ ...payload, gameId: "other-game" })
                    const result = matchReducer(state, action)
                    expect(result.matchState.data?.table).toStrictEqual(round.table)
                })
            })

            describe("Leading suit", () => {
                it("Should be cleared", () => {
                    const state = { ...defaultState, matchState: roundData }
                    const action = nextTurnAction(payload)
                    const result = matchReducer(state, action)
                    expect(result.matchState.data?.state.leadingSuit).toBe(null)
                })

                it("Should not be cleared if game has different id", () => {
                    const state = { ...defaultState, matchState: roundData }
                    const action = nextTurnAction({ ...payload, gameId: "other-game" })
                    const result = matchReducer(state, action)
                    expect(result.matchState.data?.state.leadingSuit).toStrictEqual(round.state.leadingSuit)
                })
            })

            describe("Current player", () => {
                it("should be updated", () => {
                    const state = { ...defaultState, matchState: roundData }
                    const action = nextTurnAction(payload)
                    const result = matchReducer(state, action)
                    expect(result.matchState.data?.state.currentPlayer).toEqual(payload.player)
                })

                it("should not be be updated if game has different id", () => {
                    const state = { ...defaultState, matchState: roundData }
                    const action = nextTurnAction({ ...payload, gameId: "other-game" })
                    const result = matchReducer(state, action)
                    expect(result.matchState.data?.state.currentPlayer).toEqual(round.state.currentPlayer)
                })
            })
        })
    })

    describe("Player scores", () => {
        describe("On threat status assigned", () => {
            const payload = { gameId: "foo-bar", cardNumber: 1, newStatus: true, playerId: "111" }

            it("should be increased", () => {
                const state = { ...defaultState, scores }
                const action = threatStatusAssignedAction(payload)
                const result = matchReducer(state, action)
                expect(result.scores).toStrictEqual({ ...scores, "111": scores["111"] + 1 })
            })

            it("should not be increased if threat is not linked", () => {
                const state = { ...defaultState, scores }
                const action = threatStatusAssignedAction({ ...payload, newStatus: false })
                const result = matchReducer(state, action)
                expect(result.scores).toStrictEqual(scores)
            })

            it("should be created new entry with value 1 if not exists", () => {
                const state = { ...defaultState, scores }
                const action = threatStatusAssignedAction({ ...payload, playerId: "999" })
                const result = matchReducer(state, action)
                expect(result.scores).toStrictEqual({ ...scores, "999": 1 })
            })
        })

        describe("On player takes a trick", () => {
            const payload = { gameId: "foo-bar", player: "111" }

            it("should be increased", () => {
                const state = { ...defaultState, scores }
                const action = playerTakesTrickAction(payload)
                const result = matchReducer(state, action)
                expect(result.scores).toStrictEqual({ ...scores, "111": scores["111"] + 1 })
            })

            it("should not be increased if nobody takes a trick", () => {
                const state = { ...defaultState, scores }
                const action = playerTakesTrickAction({ ...payload, player: null })
                const result = matchReducer(state, action)
                expect(result.scores).toStrictEqual(scores)
            })

            it("should be created new entry with value 1 if not exists", () => {
                const state = { ...defaultState, scores }
                const action = playerTakesTrickAction({ ...payload, player: "999" })
                const result = matchReducer(state, action)
                expect(result.scores).toStrictEqual({ ...scores, "999": 1 })
            })
        })

        describe("On refresh state", () => {
            describe("it should updated scores", () => {
                const state = { ...defaultState, scores }
                const newScores = { "111": 22, "777": 9, "999": 4 }
                const action = fetchMatchStateAction.done({
                    params: "foo-bar",
                    result: { ...round, playersScores: newScores },
                })
                const result = matchReducer(state, action)
                expect(result.scores).toStrictEqual(newScores)
            })
        })

        describe("On fetch scores", () => {
            const state = { ...defaultState, scores }
            const newScores = { "111": 22, "777": 9, "999": 4 }
            const action = fetchScoresAction.done({ params: "foo-bar", result: newScores })
            const result = matchReducer(state, action)
            expect(result.scores).toStrictEqual(newScores)
        })
    })

    describe("Player takes the trick", () => {
        describe("On event", () => {
            const payload = { gameId: "foo-bar", player: "111" }

            it("should be updated", () => {
                const state = { ...defaultState, playerTakesTrick: null }
                const action = playerTakesTrickAction(payload)
                const result = matchReducer(state, action)
                expect(result.playerTakesTrick).toStrictEqual(payload)
            })
        })
    })
})
