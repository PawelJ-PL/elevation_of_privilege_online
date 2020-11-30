import { WebSocketGameMessage } from "./types/WebSocketGameMessage"
import { gameWebsocketMessageHandlerEpics } from "./handlers/GameWsHandlers"
import { combineEpics } from "redux-observable"
import { catchError, delay, map, mergeMap, switchMap, takeUntil } from "rxjs/operators"
import { AppState } from "./../../../../application/store/index"
import { Epic } from "redux-observable"
import { AnyAction } from "redux"
import { filter } from "rxjs/operators"
import {
    gameAnteroomWsConnectedAction,
    newGameWsMessageAction,
    sendGameWsMessageAction,
    startAnteroomWsConnectionAction,
    stopAnteroomWsConnectionAction,
} from "./Actions"
import { concat, EMPTY, interval, of, Subject } from "rxjs"
import { webSocket, WebSocketSubject } from "rxjs/webSocket"

const wsProto = window.location.protocol === "https:" ? "wss" : "ws"
const baseUrl = `${wsProto}://${window.location.host}/api/v1/ws/games`

const onOpenSubject = new Subject()

const wsConnectedEpic: Epic<AnyAction, AnyAction, AppState> = (action$) =>
    action$.pipe(
        switchMap(() =>
            onOpenSubject.pipe(
                map(() => {
                    return gameAnteroomWsConnectedAction()
                })
            )
        )
    )

let wsConnectionSubject: WebSocketSubject<WebSocketGameMessage> | undefined = undefined

const createAnteroomSocket: (gameId: string) => WebSocketSubject<WebSocketGameMessage> = (gameId: string) => {
    wsConnectionSubject = webSocket({ url: `${baseUrl}/${gameId}/anteroom`, openObserver: onOpenSubject })
    return wsConnectionSubject
}

const connectAnteroomEpic: Epic<AnyAction, AnyAction, AppState> = (action$) =>
    action$.pipe(
        filter(startAnteroomWsConnectionAction.match),
        switchMap((a) =>
            createAnteroomSocket(a.payload).pipe(
                mergeMap((m) => {
                    if ("eventType" in m) {
                        return of(newGameWsMessageAction(m))
                    } else {
                        return EMPTY
                    }
                }),
                takeUntil(
                    action$.pipe(
                        filter(stopAnteroomWsConnectionAction.match),
                        filter((action) => action.payload === a.payload)
                    )
                ),
                catchError((err) => {
                    console.error(err)
                    return concat(EMPTY.pipe(delay(2000)), of(startAnteroomWsConnectionAction(a.payload)))
                })
            )
        ),
        catchError((err) => {
            console.error(err)
            return EMPTY
        })
    )

const keepAliveEpic: Epic<AnyAction, AnyAction, AppState> = (action$) =>
    action$.pipe(
        filter(startAnteroomWsConnectionAction.match),
        switchMap(() => {
            return interval(15000).pipe(
                map(() => sendGameWsMessageAction({ query: "Keepalive" })),
                takeUntil(action$.ofType(stopAnteroomWsConnectionAction))
            )
        })
    )

const sendMessageEpic: Epic<AnyAction, AnyAction, AppState> = (action$) =>
    action$.pipe(
        filter(sendGameWsMessageAction.match),
        mergeMap((message) => {
            if (!wsConnectionSubject) {
                console.warn("Unable to send WebSocket message, because subject is not ready")
                return EMPTY
            } else {
                wsConnectionSubject.next(message.payload)
                return EMPTY
            }
        }),
        catchError((err) => {
            console.error(err)
            return EMPTY
        })
    )

export const gameWsEpics = combineEpics(
    connectAnteroomEpic,
    gameWebsocketMessageHandlerEpics,
    wsConnectedEpic,
    keepAliveEpic,
    sendMessageEpic
)
