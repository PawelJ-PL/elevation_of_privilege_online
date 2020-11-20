import { UserRoleChanged } from "./../types/Events"
import { UserNotAccepted, UserRemoved } from "./../types/Errors"
import { OperationStatus } from "./../../../application/store/async/AsyncOperationResult"
import { verifyEpic } from "../../../testutils/epicsUtils"
import { game } from "./../../../testutils/constants/game"
import {
    createGameAction,
    fetchGameInfoAction,
    gameStartedAction,
    userRemovedAction,
    userRoleChangedAction,
} from "./Actions"
import { gamesEpics } from "./Epics"
import { AppState } from "../../../application/store"
import { push } from "connected-react-router"

const state = {} as AppState

const session = { userId: "111", createdAt: "2020-11-23T23:12:00Z" }
describe("Game epics", () => {
    it("should redirect to created game", () => {
        const params = { ownerNickname: "Tiger", description: "My game" }
        const action = createGameAction.done({ params, result: game })
        const expectedMarbles = "-a"
        const expectedValues = {
            a: push("/game/foo-bar"),
        }
        verifyEpic(action, gamesEpics, state, { marbles: expectedMarbles, values: expectedValues })
    })

    describe("Error on user kick", () => {
        it("should trigger action if current status is User Not Accepted", () => {
            const updatedState = {
                ...state,
                games: {
                    ...state.games,
                    fetchStatus: {
                        status: OperationStatus.FAILED,
                        params: "foo-bar",
                        error: new UserNotAccepted("User not accepted"),
                    },
                },
            }
            const payload = { gameId: "foo-bar", userId: "111" }
            const action = userRemovedAction(payload)
            const expectedMarbles = "-a"
            const expectedValues = {
                a: fetchGameInfoAction.failed({
                    params: "foo-bar",
                    error: new UserRemoved(`User 111 removed from game foo-bar`),
                }),
            }
            verifyEpic(action, gamesEpics, updatedState, { marbles: expectedMarbles, values: expectedValues })
        })

        it("should trigger action if current user is the same than removed", () => {
            const updatedState = {
                ...state,
                games: {
                    ...state.games,
                    fetchStatus: {
                        status: OperationStatus.FINISHED,
                        params: "foo-bar",
                        data: game,
                    },
                },
                users: {
                    ...state.users,
                    current: {
                        status: OperationStatus.FINISHED,
                        data: session,
                    },
                },
            }
            const payload = { gameId: "foo-bar", userId: "111" }
            const action = userRemovedAction(payload)
            const expectedMarbles = "-a"
            const expectedValues = {
                a: fetchGameInfoAction.failed({
                    params: "foo-bar",
                    error: new UserRemoved(`User 111 removed from game foo-bar`),
                }),
            }
            verifyEpic(action, gamesEpics, updatedState, { marbles: expectedMarbles, values: expectedValues })
        })

        it("should do nothing if current user is different than removed", () => {
            const updatedState = {
                ...state,
                games: {
                    ...state.games,
                    fetchStatus: {
                        status: OperationStatus.FINISHED,
                        params: "foo-bar",
                        data: game,
                    },
                },
                users: {
                    ...state.users,
                    current: {
                        status: OperationStatus.FINISHED,
                        data: { ...session, userId: "222" },
                    },
                },
            }
            const payload = { gameId: "foo-bar", userId: "111" }
            const action = userRemovedAction(payload)
            const expectedMarbles = "---"
            verifyEpic(action, gamesEpics, updatedState, { marbles: expectedMarbles })
        })
    })

    describe("User role change", () => {
        it("should trigger game refresh if current status is User Not Accepted", () => {
            const updatedState = {
                ...state,
                games: {
                    ...state.games,
                    fetchStatus: {
                        status: OperationStatus.FAILED,
                        params: "foo-bar",
                        error: new UserNotAccepted("User not accepted"),
                    },
                },
            }
            const payload: UserRoleChanged = { gameId: "foo-bar", userId: "111", role: "Player" }
            const action = userRoleChangedAction(payload)
            const expectedMarbles = "-a"
            const expectedValues = {
                a: fetchGameInfoAction.started("foo-bar"),
            }
            verifyEpic(action, gamesEpics, updatedState, { marbles: expectedMarbles, values: expectedValues })
        })

        it("should do nothing if current game is different than updated", () => {
            const updatedState = {
                ...state,
                games: {
                    ...state.games,
                    fetchStatus: {
                        status: OperationStatus.FAILED,
                        params: "other-game",
                        error: new UserNotAccepted("User not accepted"),
                    },
                },
            }
            const payload: UserRoleChanged = { gameId: "foo-bar", userId: "111", role: "Player" }
            const action = userRoleChangedAction(payload)
            const expectedMarbles = "---"
            verifyEpic(action, gamesEpics, updatedState, { marbles: expectedMarbles })
        })
    })

    describe("Game started", () => {
        it("should trigger refresh", () => {
            const updatedState = {
                ...state,
                games: {
                    ...state.games,
                    fetchStatus: {
                        status: OperationStatus.FINISHED,
                        params: "foo-bar",
                        data: game,
                    },
                },
            }
            const payload = { gameId: "foo-bar" }
            const action = gameStartedAction(payload)
            const expectedMarbles = "-a"
            const expectedValues = {
                a: fetchGameInfoAction.started("foo-bar"),
            }
            verifyEpic(action, gamesEpics, updatedState, { marbles: expectedMarbles, values: expectedValues })
        })

        it("should trigger notfing if current game is different than started", () => {
            const updatedState = {
                ...state,
                games: {
                    ...state.games,
                    fetchStatus: {
                        status: OperationStatus.FINISHED,
                        params: "other-game",
                        data: game,
                    },
                },
            }
            const payload = { gameId: "foo-bar" }
            const action = gameStartedAction(payload)
            const expectedMarbles = "---"
            verifyEpic(action, gamesEpics, updatedState, { marbles: expectedMarbles })
        })
    })
})
