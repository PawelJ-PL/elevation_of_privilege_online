import { Card } from "./Card"

export type UsersCard = {
    gameId: string
    playerId: string
    card: Card
    location: CardLocation
    threatLinked?: boolean | null
}

export type CardLocation = "Hand" | "Table" | "Out"
