import { verifyAsyncEpic } from "../../../testutils/epicsUtils"
import { createEpic } from "./AsyncActionCreator"
import { actionCreatorFactory } from "typescript-fsa"
import { AppState } from ".."
const asyncAction = actionCreatorFactory("EPIC_TEST").async<{ foo: string; bar: number }, string, Error>("TRIGGER")
const state = {} as AppState
const params = { foo: "BaZ", bar: 123 }

describe("Async epic creator", () => {
    it("should set state to finished", (done) => {
        const epic = createEpic(asyncAction, () => Promise.resolve("FooBar"))
        const trigger = asyncAction.started(params)
        verifyAsyncEpic(trigger, epic, state, asyncAction.done({ params, result: "FooBar" }), done)
    })

    it("should set state to failed", (done) => {
        const epic = createEpic(asyncAction, () => Promise.reject(new Error("Some error")))
        const trigger = asyncAction.started(params)
        verifyAsyncEpic(trigger, epic, state, asyncAction.failed({ params, error: new Error("Some error") }), done)
    })
})
