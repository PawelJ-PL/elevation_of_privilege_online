import {
    cardPlayedAction,
    gameFinishedAction,
    nextPlayerAction,
    nextTurnAction,
    playerTakesTrickAction,
    threatStatusAssignedAction,
} from "./../../Actions"
import { AppState } from "./../../../../../application/store/index"
import { AnyAction } from "redux"
import { combineEpics, Epic } from "redux-observable"
import { filter, mergeMap } from "rxjs/operators"
import { matchWsNewMessageAction } from "../Actions"
import { EMPTY, of } from "rxjs"
import { gameDeletedAction } from "../../../../game/store/Actions"

const threatStatusAssignedEpic: Epic<AnyAction, AnyAction, AppState> = (action$) =>
    action$.pipe(
        filter(matchWsNewMessageAction.match),
        mergeMap((message) => {
            if (message.payload.eventType === "ThreatStatusAssigned") {
                return of(threatStatusAssignedAction(message.payload.payload))
            } else {
                return EMPTY
            }
        })
    )

const nextPlayerEpic: Epic<AnyAction, AnyAction, AppState> = (action$) =>
    action$.pipe(
        filter(matchWsNewMessageAction.match),
        mergeMap((message) => {
            if (message.payload.eventType === "NextPlayer") {
                return of(nextPlayerAction(message.payload.payload))
            } else {
                return EMPTY
            }
        })
    )

const cardPlayedEpic: Epic<AnyAction, AnyAction, AppState> = (action$) =>
    action$.pipe(
        filter(matchWsNewMessageAction.match),
        mergeMap((message) => {
            if (message.payload.eventType === "CardPlayed") {
                return of(cardPlayedAction(message.payload.payload))
            } else {
                return EMPTY
            }
        })
    )

const nextTurnEpic: Epic<AnyAction, AnyAction, AppState> = (action$) =>
    action$.pipe(
        filter(matchWsNewMessageAction.match),
        mergeMap((message) => {
            if (message.payload.eventType === "NextRound") {
                return of(nextTurnAction(message.payload.payload))
            } else {
                return EMPTY
            }
        })
    )

const gameFinishedEpic: Epic<AnyAction, AnyAction, AppState> = (action$) =>
    action$.pipe(
        filter(matchWsNewMessageAction.match),
        mergeMap((message) => {
            if (message.payload.eventType === "GameFinished") {
                return of(gameFinishedAction(message.payload.payload))
            } else {
                return EMPTY
            }
        })
    )

const playerTakesTrickEpic: Epic<AnyAction, AnyAction, AppState> = (action$) =>
    action$.pipe(
        filter(matchWsNewMessageAction.match),
        mergeMap((message) => {
            if (message.payload.eventType === "PlayerTakesTrick") {
                return of(playerTakesTrickAction(message.payload.payload))
            } else {
                return EMPTY
            }
        })
    )

const gameDeletedEpic: Epic<AnyAction, AnyAction, AppState> = (action$) =>
    action$.pipe(
        filter(matchWsNewMessageAction.match),
        mergeMap((message) => {
            if (message.payload.eventType === "GameDeleted") {
                return of(gameDeletedAction(message.payload.payload))
            } else {
                return EMPTY
            }
        })
    )

export const matchWebSocketMessageHandlerEpic = combineEpics(
    threatStatusAssignedEpic,
    nextPlayerEpic,
    cardPlayedEpic,
    nextTurnEpic,
    gameFinishedEpic,
    playerTakesTrickEpic,
    gameDeletedEpic
)
