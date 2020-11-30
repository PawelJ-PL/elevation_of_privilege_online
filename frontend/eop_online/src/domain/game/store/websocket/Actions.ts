import { WsGameMessageIn, WsGameMessageOut } from "./types/WebSocketGameMessage"
import actionCreatorFactory from "typescript-fsa"

const actionCreator = actionCreatorFactory("GameWS")

export const startAnteroomWsConnectionAction = actionCreator<string>("START_ANTEROOM_WS_CONNECTION")
export const stopAnteroomWsConnectionAction = actionCreator<string>("STOP_ANTEROOM_WS_CONNECTION")
export const newGameWsMessageAction = actionCreator<WsGameMessageOut>("NEW_GAME_WS_MESSAGE")
export const gameAnteroomWsConnectedAction = actionCreator("GAME_ANTEROOM_WS_CONNECTED")
export const sendGameWsMessageAction = actionCreator<WsGameMessageIn>("SEND_GAME_WS_MESSAGE")
