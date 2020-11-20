import { MemberRole } from "./../../../types/Member"
import { gameStartedAction, newParticipantAction, userRemovedAction, userRoleChangedAction } from "./../../Actions"
import { verifyEpic } from "../../../../../testutils/epicsUtils"
import { AppState } from "../../../../../application/store"
import { newGameWsMessageAction } from "../Actions"
import { gameWebsocketMessageHandlerEpics } from "./GameWsHandlers"

const state = {} as AppState

describe("Game WebSocket handlers", () => {
    it("should generate user role changed action", () => {
        const role: MemberRole = "Player"
        const payload = { gameId: "foo-bar", userId: "123", role }
        const action = newGameWsMessageAction({
            eventType: "UserRoleChanged",
            payload,
        })
        const expectedMarbles = "-a"
        const expectedValues = {
            a: userRoleChangedAction(payload),
        }
        verifyEpic(action, gameWebsocketMessageHandlerEpics, state, {
            marbles: expectedMarbles,
            values: expectedValues,
        })
    })

    it("should generate participant removed action", () => {
        const payload = { gameId: "foo-bar", userId: "123" }
        const action = newGameWsMessageAction({
            eventType: "ParticipantRemoved",
            payload,
        })
        const expectedMarbles = "-a"
        const expectedValues = {
            a: userRemovedAction(payload),
        }
        verifyEpic(action, gameWebsocketMessageHandlerEpics, state, {
            marbles: expectedMarbles,
            values: expectedValues,
        })
    })

    it("should generate new participant action", () => {
        const payload = { gameId: "foo-bar", userId: "123", nickName: "Tiger" }
        const action = newGameWsMessageAction({
            eventType: "NewParticipant",
            payload,
        })
        const expectedMarbles = "-a"
        const expectedValues = {
            a: newParticipantAction(payload),
        }
        verifyEpic(action, gameWebsocketMessageHandlerEpics, state, {
            marbles: expectedMarbles,
            values: expectedValues,
        })
    })

    it("should generate game started action", () => {
        const payload = { gameId: "foo-bar" }
        const action = newGameWsMessageAction({
            eventType: "GameStarted",
            payload,
        })
        const expectedMarbles = "-a"
        const expectedValues = {
            a: gameStartedAction(payload),
        }
        verifyEpic(action, gameWebsocketMessageHandlerEpics, state, {
            marbles: expectedMarbles,
            values: expectedValues,
        })
    })
})
