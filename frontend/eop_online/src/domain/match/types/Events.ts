import { Card } from "./Card"
export type ThreatStatusAssigned = { gameId: string; cardNumber: number; newStatus: boolean; playerId: string }

export type NextPlayer = { gameId: string; newPlayer: string }

export type CardPlayed = {
    gameId: string
    playerId: string
    card: Card
    location: "Table"
    threatLinked?: null
}

export type NextTurn = { gameId: string; player: string }

export type GameFinished = { gameId: string }

export type PlayerTakesTrick = { gameId: string; player?: string | null }
