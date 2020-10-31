import actionCreatorFactory from "typescript-fsa"
import { WebSocketMatchMessage } from "./types/WebSocketMatchMessage"

const actionCreator = actionCreatorFactory("matchWS")

export const startMatchWsConnectionAction = actionCreator<string>("START_MATCH_WS_CONNECTION")
export const stopMatchWsConnectionAction = actionCreator<string>("STOP_MATCH_WS_CONNECTION")
export const matchWsNewMessageAction = actionCreator<WebSocketMatchMessage>("MATCH_WS_NEW_MESSAGE")
export const matchWsConnectedAction = actionCreator("MATCH_WS_CONNECTED")
