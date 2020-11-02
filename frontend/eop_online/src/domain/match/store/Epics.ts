import { fetchGameInfoAction } from "./../../game/store/Actions"
import { EMPTY, of } from "rxjs"
import { matchWsConnectedAction } from "./websocket/Actions"
import { AppState } from "./../../../application/store/index"
import { AnyAction } from "redux"
import { matchWsEpic } from "./websocket/Epics"
import { combineEpics, Epic } from "redux-observable"
import { createEpic } from "../../../application/store/async/AsyncActionErrorCreator"
import MatchesApi from "../api/MatchesApi"
import { Round } from "../types/Round"
import {
    fetchMatchStateAction,
    fetchScoresAction,
    gameFinishedAction,
    putCardOnTableAction,
    PutCardPayload,
    updateCardOnTableAction,
    UpdateCardPayload,
} from "./Actions"
import { filter, mergeMap } from "rxjs/operators"

const fetchMatchStateEpic = createEpic<string, Round, Error>(fetchMatchStateAction, (params) =>
    MatchesApi.fetchMatchState(params)
)

const updateCardOnTableEpic = createEpic<UpdateCardPayload, void, Error>(updateCardOnTableAction, (params) =>
    MatchesApi.updateCardOnTable(params.matchId, params.cardNumber, params.data)
)

const putCardOnTableEpic = createEpic<PutCardPayload, void, Error>(putCardOnTableAction, (params) =>
    MatchesApi.putCardOnTable(params.matchId, params.cardNumber)
)

const refreshStateOnWsReconnect: Epic<AnyAction, AnyAction, AppState> = (action$, state$) =>
    action$.pipe(
        filter(matchWsConnectedAction.match),
        mergeMap(() => {
            if (state$.value.matches.matchState?.params) {
                return of(fetchMatchStateAction.started(state$.value.matches.matchState.params))
            } else {
                return EMPTY
            }
        })
    )

const refreshGameOnFinishEpic: Epic<AnyAction, AnyAction, AppState> = (action$, state$) =>
    action$.pipe(
        filter(gameFinishedAction.match),
        mergeMap((action) => {
            if (state$.value.games.fetchStatus.params === action.payload.gameId) {
                return of(fetchGameInfoAction.started(action.payload.gameId))
            } else {
                return EMPTY
            }
        })
    )

const fetchScoresEpic = createEpic<string, Record<string, number>, Error>(fetchScoresAction, (params) =>
    MatchesApi.getScores(params)
)

export const matchesEpics = combineEpics(
    fetchMatchStateEpic,
    updateCardOnTableEpic,
    putCardOnTableEpic,
    matchWsEpic,
    refreshStateOnWsReconnect,
    refreshGameOnFinishEpic,
    fetchScoresEpic
)
