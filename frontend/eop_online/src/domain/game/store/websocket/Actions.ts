import { WebSocketGameMessage } from "./types/WebSocketGameMessage"
import actionCreatorFactory from "typescript-fsa"

const actionCreator = actionCreatorFactory("GameWS")

export const startAnteroomWsConnectionAction = actionCreator<string>("START_ANTEROOM_WS_CONNECTION")
export const stopAnteroomWsConnectionAction = actionCreator<string>("STOP_ANTEROOM_WS_CONNECTION")
export const newGameWsMessageAction = actionCreator<WebSocketGameMessage>("NEW_GAME_WS_MESSAGE")
export const gameAnteroomWsConnectedAction = actionCreator("GAME_ANTEROOM_WS_CONNECTED")
