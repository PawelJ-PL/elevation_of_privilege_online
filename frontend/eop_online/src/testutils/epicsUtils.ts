import { ActionsObservable, Epic, StateObservable } from "redux-observable"
import { Action } from "typescript-fsa"
import { AppState } from "../application/store"
import { TestScheduler } from "rxjs/testing"
import { Subject } from "rxjs"
import { AnyAction } from "redux"

export const verifyEpic = (
    action: Action<unknown>,
    epic: Epic<AnyAction, AnyAction, AppState>,
    state: AppState,
    expected: { marbles: string; values?: unknown; error?: unknown }
) => {
    const testScheduler = new TestScheduler((actual, expected) => expect(actual).toStrictEqual(expected))

    testScheduler.run(({ hot, expectObservable }) => {
        const input$ = hot("-a", { a: action })
        const action$ = new ActionsObservable(input$)
        const state$ = new StateObservable(new Subject<AppState>(), state)
        const result = epic(action$, state$, {})

        expectObservable(result).toBe(expected.marbles, expected.values, expected.error)
    })
}

export const verifyAsyncEpic = (
    action: Action<unknown>,
    epic: Epic<AnyAction, AnyAction, AppState>,
    state: AppState,
    expectedAction: Action<unknown>,
    done: jest.DoneCallback
) => {
    const action$ = ActionsObservable.of(action)
    const state$ = new StateObservable(new Subject<AppState>(), state)
    const result = epic(action$, state$, {})
    result.subscribe((a) => {
        expect(a).toStrictEqual(expectedAction)
        done()
    })
}
