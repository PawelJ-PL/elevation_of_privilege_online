import { OperationStatus } from "./AsyncOperationResult"
import { createReducer } from "./AsyncReducerCreator"
import { actionCreatorFactory } from "typescript-fsa"
const actionCreator = actionCreatorFactory("REDUCER_TEST")
const asyncAction = actionCreator.async<{ foo: string; bar: number }, string, Error>("TRIGGER")
const resetAction = actionCreator("REDUCER_RESET")
const params = { foo: "BaZ", bar: 123 }

describe("Async operations reducer", () => {
    it("should set status PENDING", () => {
        const reducer = createReducer(asyncAction)
        const result = reducer(undefined, asyncAction.started(params))
        expect(result).toStrictEqual({ status: OperationStatus.PENDING, params, data: undefined, error: undefined })
    })

    it("should set status FINISHED", () => {
        const reducer = createReducer(asyncAction)
        const result = reducer(undefined, asyncAction.done({ params, result: "FooBar" }))
        expect(result).toStrictEqual({ status: OperationStatus.FINISHED, params, data: "FooBar", error: undefined })
    })

    it("should set status FAILED", () => {
        const reducer = createReducer(asyncAction)
        const result = reducer(undefined, asyncAction.failed({ params, error: new Error("Some error") }))
        expect(result).toStrictEqual({
            status: OperationStatus.FAILED,
            params,
            data: undefined,
            error: new Error("Some error"),
        })
    })

    it("should reset status on reset action", () => {
        const reducer = createReducer(asyncAction, resetAction)
        const init = reducer(undefined, asyncAction.started(params))
        const result = reducer(init, resetAction())
        expect(result).toStrictEqual({
            status: OperationStatus.NOT_STARTED,
            params: undefined,
            data: undefined,
            error: undefined,
        })
    })
})
