import { CardPlayed } from "./../../../types/Events"
import {
    cardPlayedAction,
    gameFinishedAction,
    nextPlayerAction,
    nextTurnAction,
    playerTakesTrickAction,
    threatStatusAssignedAction,
} from "./../../Actions"
import { matchWebSocketMessageHandlerEpic } from "./MatchWsHandlers"
import { verifyEpic } from "./../../../../../testutils/epicsUtils"
import { matchWsNewMessageAction } from "../Actions"
import { AppState } from "../../../../../application/store"

const defaultState = {} as AppState

describe("Match WebSocket Handlers", () => {
    it("should generate Threat Status Assigned action", () => {
        const payload = { gameId: "foo-bar", cardNumber: 7, newStatus: false, playerId: "111" }
        const action = matchWsNewMessageAction({ eventType: "ThreatStatusAssigned", payload })
        const expectedMarbles = "-a"
        const expectedValues = {
            a: threatStatusAssignedAction(payload),
        }
        verifyEpic(action, matchWebSocketMessageHandlerEpic, defaultState, {
            marbles: expectedMarbles,
            values: expectedValues,
        })
    })

    it("should generate Next Players action", () => {
        const payload = { gameId: "foo-bar", newPlayer: "111" }
        const action = matchWsNewMessageAction({ eventType: "NextPlayer", payload })
        const expectedMarbles = "-a"
        const expectedValues = {
            a: nextPlayerAction(payload),
        }
        verifyEpic(action, matchWebSocketMessageHandlerEpic, defaultState, {
            marbles: expectedMarbles,
            values: expectedValues,
        })
    })

    it("should generate Card Played action", () => {
        const payload: CardPlayed = {
            gameId: "foo-bar",
            playerId: "111",
            card: { cardNumber: 7, value: "Eight", suit: "Spoofing", text: "Lorem ipsum" },
            location: "Table",
            threatLinked: null,
        }
        const action = matchWsNewMessageAction({ eventType: "CardPlayed", payload })
        const expectedMarbles = "-a"
        const expectedValues = {
            a: cardPlayedAction(payload),
        }
        verifyEpic(action, matchWebSocketMessageHandlerEpic, defaultState, {
            marbles: expectedMarbles,
            values: expectedValues,
        })
    })

    it("should generate Next Turn action", () => {
        const payload = { gameId: "foo-bar", player: "111" }
        const action = matchWsNewMessageAction({ eventType: "NextRound", payload })
        const expectedMarbles = "-a"
        const expectedValues = {
            a: nextTurnAction(payload),
        }
        verifyEpic(action, matchWebSocketMessageHandlerEpic, defaultState, {
            marbles: expectedMarbles,
            values: expectedValues,
        })
    })

    it("should generate Game Finished action", () => {
        const payload = { gameId: "foo-bar" }
        const action = matchWsNewMessageAction({ eventType: "GameFinished", payload })
        const expectedMarbles = "-a"
        const expectedValues = {
            a: gameFinishedAction(payload),
        }
        verifyEpic(action, matchWebSocketMessageHandlerEpic, defaultState, {
            marbles: expectedMarbles,
            values: expectedValues,
        })
    })

    it("should generate Player Takes Trick action", () => {
        const payload = { gameId: "foo-bar", player: "111" }
        const action = matchWsNewMessageAction({ eventType: "PlayerTakesTrick", payload })
        const expectedMarbles = "-a"
        const expectedValues = {
            a: playerTakesTrickAction(payload),
        }
        verifyEpic(action, matchWebSocketMessageHandlerEpic, defaultState, {
            marbles: expectedMarbles,
            values: expectedValues,
        })
    })
})
