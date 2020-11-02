import { UsersCard } from "./UsersCard"
import { Card } from "./Card"
import { RoundState } from "./RoundState"

export type Round = {
    state: RoundState
    hand: Card[]
    table: UsersCard[]
    playersScores: Record<string, number>
}
