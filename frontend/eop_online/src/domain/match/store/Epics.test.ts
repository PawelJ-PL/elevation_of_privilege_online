import { OperationStatus } from "./../../../application/store/async/AsyncOperationResult"
import { matchesEpics } from "./Epics"
import { verifyEpic } from "./../../../testutils/epicsUtils"
import { AppState } from "../../../application/store"
import { matchWsConnectedAction } from "./websocket/Actions"
import { fetchMatchStateAction, gameFinishedAction } from "./Actions"
import { fetchGameInfoAction } from "../../game/store/Actions"

const defaultState = {} as AppState

describe("Match epics", () => {
    describe("On WebSocket connection", () => {
        it("should refresh game state", () => {
            const state = {
                ...defaultState,
                matches: {
                    ...defaultState.matches,
                    matchState: { status: OperationStatus.FINISHED, params: "foo-bar" },
                },
            }
            const action = matchWsConnectedAction()
            const expectedMarbles = "-a"
            const expectedValues = {
                a: fetchMatchStateAction.started("foo-bar"),
            }
            verifyEpic(action, matchesEpics, state, { marbles: expectedMarbles, values: expectedValues })
        })

        it("should not refresh game state if current state has no request params", () => {
            const state = {
                ...defaultState,
                matches: {
                    ...defaultState.matches,
                    matchState: { status: OperationStatus.FINISHED },
                },
            }
            const action = matchWsConnectedAction()
            const expectedMarbles = "---"
            verifyEpic(action, matchesEpics, state, { marbles: expectedMarbles })
        })
    })

    describe("On game finished", () => {
        it("should refresh game info", () => {
            const state = {
                ...defaultState,
                games: { ...defaultState.games, fetchStatus: { status: OperationStatus.FINISHED, params: "foo-bar" } },
            }
            const action = gameFinishedAction({ gameId: "foo-bar" })
            const expectedMarbles = "-a"
            const expectedValues = {
                a: fetchGameInfoAction.started("foo-bar"),
            }
            verifyEpic(action, matchesEpics, state, { marbles: expectedMarbles, values: expectedValues })
        })

        it("should not refresh game info if current game has different id", () => {
            const state = {
                ...defaultState,
                games: { ...defaultState.games, fetchStatus: { status: OperationStatus.FINISHED, params: "foo-bar" } },
            }
            const action = gameFinishedAction({ gameId: "other-game" })
            const expectedMarbles = "---"
            verifyEpic(action, matchesEpics, state, { marbles: expectedMarbles })
        })
    })
})
