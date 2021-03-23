import { GameDeleted, GameStarted, NewParticipant, UserRemoved, UserRoleChanged } from "./../../../types/Events"

export type WebSocketGameMessage = WsGameMessageOut | WsGameMessageIn

export type WsGameMessageOut =
    | { eventType: "UserRoleChanged"; payload: UserRoleChanged }
    | { eventType: "ParticipantRemoved"; payload: UserRemoved }
    | { eventType: "NewParticipant"; payload: NewParticipant }
    | { eventType: "GameStarted"; payload: GameStarted }
    | { eventType: "GameDeleted"; payload: GameDeleted }

export type WsGameMessageIn = { query: "Keepalive" }
