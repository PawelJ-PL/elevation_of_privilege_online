import {
    CardPlayed,
    GameFinished,
    NextPlayer,
    NextTurn,
    PlayerTakesTrick,
    ThreatStatusAssigned,
} from "./../../../types/Events"
export type WebSocketMatchMessage = WsMatchMessageOut | WsMatchMessageIn

export type WsMatchMessageOut =
    | { eventType: "ThreatStatusAssigned"; payload: ThreatStatusAssigned }
    | { eventType: "NextPlayer"; payload: NextPlayer }
    | { eventType: "CardPlayed"; payload: CardPlayed }
    | { eventType: "NextRound"; payload: NextTurn }
    | { eventType: "GameFinished"; payload: GameFinished }
    | { eventType: "PlayerTakesTrick"; payload: PlayerTakesTrick }

export type WsMatchMessageIn = { query: "Keepalive" }
