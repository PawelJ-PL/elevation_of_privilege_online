import { UserGameSummary } from "./../types/UserGameSummary"
import { AppState } from "./../../../application/store/index"
import { UserNotAccepted, UserRemoved } from "./../types/Errors"
import { Game } from "./../types/Game"
import {
    assignUserRoleAction,
    createGameAction,
    deleteGameAction,
    fetchGameInfoAction,
    fetchMembersAction,
    fetchUserGamesAction,
    gameDeletedAction,
    gameStartedAction,
    joinGameAction,
    kickUserAction,
    startGameAction,
    userRemovedAction,
    userRoleChangedAction,
} from "./Actions"
import { createEpic } from "../../../application/store/async/AsyncActionCreator"
import { combineEpics, Epic } from "redux-observable"
import { Action, AnyAction } from "redux"
import GamesApi from "../api/GamesApi"
import { filter, map, mergeMap } from "rxjs/operators"
import { push } from "connected-react-router"
import { Member, MemberRole } from "../types/Member"
import { gameWsEpics } from "./websocket/Epics"
import { EMPTY, of } from "rxjs"

const createGameEpic = createEpic<{ ownerNickname: string; description?: string | null }, Game, Error>(
    createGameAction,
    (params) => GamesApi.createGame(params.ownerNickname, params.description)
)

const newGameRedirectEpic: Epic<Action, Action, AppState> = (action$) =>
    action$.pipe(
        filter(createGameAction.done.match),
        map((action) => push("/game/" + action.payload.result.id))
    )

const fetchGameStatusEpic = createEpic<string, Game | null, Error>(fetchGameInfoAction, (params) =>
    GamesApi.getGameInfo(params)
)

const joinGameEpic = createEpic<{ gameId: string; nickname: string }, void, Error>(joinGameAction, (params) =>
    GamesApi.joinGame(params.gameId, params.nickname)
)

const fetchMembersEpic = createEpic<string, Member[], Error>(fetchMembersAction, (params) =>
    GamesApi.fetchMembers(params)
)

const assignRoleEpic = createEpic<{ gameId: string; participantId: string; role: MemberRole }, void, Error>(
    assignUserRoleAction,
    (params) => GamesApi.assignRole(params.gameId, params.participantId, params.role)
)

const kickUserEpic = createEpic<{ gameId: string; participantId: string }, void, Error>(kickUserAction, (params) =>
    GamesApi.kickUser(params.gameId, params.participantId)
)

const errorOnUserKickEpic: Epic<AnyAction, AnyAction, AppState> = (action$, state$) =>
    action$.pipe(
        filter(userRemovedAction.match),
        mergeMap((a) => {
            if (
                (state$.value.games.fetchStatus.error instanceof UserNotAccepted ||
                    state$.value.users.current.data?.userId === a.payload.userId) &&
                state$.value.games.fetchStatus.params === a.payload.gameId
            ) {
                return of(
                    fetchGameInfoAction.failed({
                        params: state$.value.games.fetchStatus.params,
                        error: new UserRemoved(`User ${a.payload.userId} removed from game ${a.payload.gameId}`),
                    })
                )
            }
            return EMPTY
        })
    )

const refreshGameOnAcceptEpic: Epic<AnyAction, AnyAction, AppState> = (action$, state$) =>
    action$.pipe(
        filter(userRoleChangedAction.match),
        mergeMap((a) => {
            if (
                state$.value.games.fetchStatus.error instanceof UserNotAccepted &&
                state$.value.games.fetchStatus.params === a.payload.gameId
            ) {
                return of(fetchGameInfoAction.started(a.payload.gameId))
            } else {
                return EMPTY
            }
        })
    )

const startGameEpic = createEpic<string, void, Error>(startGameAction, (params) => GamesApi.startGame(params))

const refreshGameOnStartEpic: Epic<AnyAction, AnyAction, AppState> = (action$, state$) =>
    action$.pipe(
        filter(gameStartedAction.match),
        mergeMap((a) => {
            if (state$.value.games.fetchStatus.params === a.payload.gameId) {
                return of(fetchGameInfoAction.started(a.payload.gameId))
            } else {
                return EMPTY
            }
        })
    )

const fetchAvailableGamesEpic = createEpic<void, UserGameSummary[], Error>(fetchUserGamesAction, () =>
    GamesApi.fetchAvailableGames()
)

const updateGameOnDeleteEpic: Epic<AnyAction, AnyAction, AppState> = (action$, state$) =>
    action$.pipe(
        filter(gameDeletedAction.match),
        mergeMap((a) => {
            if (a.payload.gameId === state$.value.games.fetchStatus.params) {
                return of(fetchGameInfoAction.done({ result: null, params: a.payload.gameId }))
            } else {
                return EMPTY
            }
        })
    )

const deleteGameEpic = createEpic<string, void, Error>(deleteGameAction, (params) => GamesApi.deleteGame(params))

export const gamesEpics = combineEpics<Action, Action, AppState>(
    createGameEpic,
    newGameRedirectEpic,
    fetchGameStatusEpic,
    joinGameEpic,
    fetchMembersEpic,
    assignRoleEpic,
    kickUserEpic,
    gameWsEpics,
    errorOnUserKickEpic,
    refreshGameOnAcceptEpic,
    startGameEpic,
    refreshGameOnStartEpic,
    fetchAvailableGamesEpic,
    deleteGameEpic,
    updateGameOnDeleteEpic
)
