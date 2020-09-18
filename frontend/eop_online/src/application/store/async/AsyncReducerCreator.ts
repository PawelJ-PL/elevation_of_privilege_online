import { ActionCreator, AsyncActionCreators } from "typescript-fsa"
import { reducerWithInitialState } from "typescript-fsa-reducers"
import AsyncOperationsResult, { OperationStatus } from "./AsyncOperationResult"

export const defaultInitState = {
    status: OperationStatus.NOT_STARTED,
    data: undefined,
    error: undefined,
    params: undefined,
}

export const createReducer = <Params, Result, Error = unknown>(
    asyncActions: AsyncActionCreators<Params, Result, Error>,
    resetAction: ActionCreator<void> | null = null,
    initialState: AsyncOperationsResult<Result, Error, Params> = defaultInitState
) => {
    const baseReducer = reducerWithInitialState(initialState)
        .case(asyncActions.started, (_, params) => ({
            status: OperationStatus.PENDING,
            data: undefined,
            error: undefined,
            params: params,
        }))
        .case(asyncActions.done, (_, action) => ({
            status: OperationStatus.FINISHED,
            data: action.result,
            error: undefined,
            params: action.params,
        }))
        .case(asyncActions.failed, (_, action) => ({
            status: OperationStatus.FAILED,
            data: undefined,
            error: action.error,
            params: action.params,
        }))

    return resetAction === null
        ? baseReducer
        : baseReducer.case(resetAction, (state) => ({
              ...state,
              ...defaultInitState,
          }))
}
