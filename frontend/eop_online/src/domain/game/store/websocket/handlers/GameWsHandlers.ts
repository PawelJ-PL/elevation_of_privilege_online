import { gameStartedAction, newParticipantAction, userRemovedAction, userRoleChangedAction } from "./../../Actions"
import { newGameWsMessageAction } from "./../Actions"
import { AppState } from "./../../../../../application/store/index"
import { AnyAction } from "redux"
import { combineEpics, Epic } from "redux-observable"
import { filter, mergeMap } from "rxjs/operators"
import { EMPTY, of } from "rxjs"

const roleChangeMessageEpic: Epic<AnyAction, AnyAction, AppState> = (action$) =>
    action$.pipe(
        filter(newGameWsMessageAction.match),
        mergeMap((message) => {
            if (message.payload.eventType === "UserRoleChanged") {
                return of(userRoleChangedAction(message.payload.payload))
            } else {
                return EMPTY
            }
        })
    )

const userKickedEpic: Epic<AnyAction, AnyAction, AppState> = (action$) =>
    action$.pipe(
        filter(newGameWsMessageAction.match),
        mergeMap((message) => {
            if (message.payload.eventType === "ParticipantRemoved") {
                return of(userRemovedAction(message.payload.payload))
            } else {
                return EMPTY
            }
        })
    )

const newMemberEpic: Epic<AnyAction, AnyAction, AppState> = (action$) =>
    action$.pipe(
        filter(newGameWsMessageAction.match),
        mergeMap((message) => {
            if (message.payload.eventType === "NewParticipant") {
                return of(newParticipantAction(message.payload.payload))
            } else {
                return EMPTY
            }
        })
    )

const gameStartedEpic: Epic<AnyAction, AnyAction, AppState> = (action$) =>
    action$.pipe(
        filter(newGameWsMessageAction.match),
        mergeMap((message) => {
            if (message.payload.eventType === "GameStarted") {
                return of(gameStartedAction(message.payload.payload))
            } else {
                return EMPTY
            }
        })
    )

export const gameWebsocketMessageHandlerEpics = combineEpics(
    roleChangeMessageEpic,
    userKickedEpic,
    newMemberEpic,
    gameStartedEpic
)
