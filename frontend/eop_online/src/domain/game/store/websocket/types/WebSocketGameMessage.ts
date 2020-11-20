import { GameStarted, NewParticipant, UserRemoved, UserRoleChanged } from "./../../../types/Events"

export type WebSocketGameMessage = WsGameMessageOut

export type WsGameMessageOut =
    | { eventType: "UserRoleChanged"; payload: UserRoleChanged }
    | { eventType: "ParticipantRemoved"; payload: UserRemoved }
    | { eventType: "NewParticipant"; payload: NewParticipant }
    | { eventType: "GameStarted"; payload: GameStarted }
