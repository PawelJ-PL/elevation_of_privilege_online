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
    startAnteroomWsConnectionAction,
    stopAnteroomWsConnectionAction,
} from "./Actions"
import { concat, EMPTY, of, Subject } from "rxjs"
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

const createAnteroomSocket: (gameId: string) => WebSocketSubject<WebSocketGameMessage> = (gameId: string) =>
    webSocket({ url: `${baseUrl}/${gameId}/anteroom`, openObserver: onOpenSubject })

const connectAnteroomEpic: Epic<AnyAction, AnyAction, AppState> = (action$) =>
    action$.pipe(
        filter(startAnteroomWsConnectionAction.match),
        switchMap((a) =>
            createAnteroomSocket(a.payload).pipe(
                mergeMap((m) => of(newGameWsMessageAction(m))),
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

export const gameWsEpics = combineEpics(connectAnteroomEpic, gameWebsocketMessageHandlerEpics, wsConnectedEpic)
