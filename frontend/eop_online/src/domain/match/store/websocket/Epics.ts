import { matchWebSocketMessageHandlerEpic } from "./handlers/MatchWsHandlers"
import {
    matchWsConnectedAction,
    matchWsNewMessageAction,
    startMatchWsConnectionAction,
    stopMatchWsConnectionAction,
} from "./Actions"
import { AnyAction } from "redux"
import { combineEpics, Epic } from "redux-observable"
import { catchError, delay, filter, map, mergeMap, switchMap, takeUntil } from "rxjs/operators"
import { AppState } from "../../../../application/store"
import { concat, EMPTY, of, Subject } from "rxjs"
import { webSocket, WebSocketSubject } from "rxjs/webSocket"
import { WebSocketMatchMessage } from "./types/WebSocketMatchMessage"

const wsProto = window.location.protocol === "https:" ? "wss" : "ws"
const baseUrl = `${wsProto}://${window.location.host}/api/v1/ws/games`

const onOpenSubject = new Subject()

const wsConnectedEpic: Epic<AnyAction, AnyAction, AppState> = (action$) =>
    action$.pipe(
        switchMap(() =>
            onOpenSubject.pipe(
                map(() => {
                    return matchWsConnectedAction()
                })
            )
        )
    )

const createMatchSocket: (matchId: string) => WebSocketSubject<WebSocketMatchMessage> = (gameId: string) =>
    webSocket({ url: `${baseUrl}/${gameId}`, openObserver: onOpenSubject })

const connectMatchEpic: Epic<AnyAction, AnyAction, AppState> = (action$) =>
    action$.pipe(
        filter(startMatchWsConnectionAction.match),
        switchMap((a) =>
            createMatchSocket(a.payload).pipe(
                mergeMap((m) => of(matchWsNewMessageAction(m))),
                takeUntil(
                    action$.pipe(
                        filter(stopMatchWsConnectionAction.match),
                        filter((action) => action.payload === a.payload)
                    )
                ),
                catchError((err) => {
                    console.error(err)
                    return concat(EMPTY.pipe(delay(2000)), of(startMatchWsConnectionAction(a.payload)))
                })
            )
        ),
        catchError((err) => {
            console.error(err)
            return EMPTY
        })
    )

export const matchWsEpic = combineEpics(connectMatchEpic, wsConnectedEpic, matchWebSocketMessageHandlerEpic)
