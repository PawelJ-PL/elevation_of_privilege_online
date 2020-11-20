import { ActionsObservable } from "redux-observable"
import { from, ObservableInput } from "rxjs"
import { AnyAction, AsyncActionCreators } from "typescript-fsa"
import { catchError, filter, map, mergeMap } from "rxjs/operators"

export const createEpic = <Params, Result, Error>(
    asyncActions: AsyncActionCreators<Params, Result, Error>,
    requestCreator: (params: Params) => ObservableInput<Result>
) => {
    return (actions$: ActionsObservable<AnyAction>) =>
        actions$.pipe(
            filter(asyncActions.started.match),
            mergeMap((action) =>
                from(requestCreator(action.payload)).pipe(
                    map((resp) => asyncActions.done({ result: resp, params: action.payload })),
                    catchError((err: Error) => [asyncActions.failed({ params: action.payload, error: err })])
                )
            )
        )
}
