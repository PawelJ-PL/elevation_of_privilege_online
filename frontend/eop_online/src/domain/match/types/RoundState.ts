import { Suit } from "./Card"

export type RoundState = {
    gameId: string
    currentPlayer: string
    leadingSuit?: Suit | null
}
