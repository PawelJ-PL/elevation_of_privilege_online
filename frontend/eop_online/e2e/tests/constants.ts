import { RoundState } from "./../../src/domain/match/types/RoundState"
import { Round } from "./../../src/domain/match/types/Round"
import { Card } from "./../../src/domain/match/types/Card"
import { UsersCard } from "../../src/domain/match/types/UsersCard"

export const myId = "28b54254-954b-4b3c-a126-76f9aa42373a"
export const player2Id = "10362f85-6553-4775-ade5-33934d9532b5"
export const player3Id = "f10e1639-b2e7-4d09-b044-333d8aca314f"
export const observer1Id = "dfc88e51-0843-4dce-8fa6-344db3e6089d"

export const exampleGame = { id: myId, description: "My game", creator: myId, startedAt: "2020-11-23T23:20:00Z" }

export const meData = { userId: myId, createdAt: "2020-11-23T23:12:00Z" }

export const initMember = { id: myId, nickname: "Pawel", role: "Player" }
export const player2 = { id: player2Id, nickname: "Alice", role: "Player" }
export const player3 = { id: player3Id, nickname: "Adam", role: "Player" }
export const observer1 = { id: observer1Id, nickname: "Bob", role: "Observer" }

export const exampleRoundState: RoundState = {
    gameId: exampleGame.id,
    currentPlayer: myId,
    leadingSuit: "Tampering",
}

type SuitKey =
    | "spoofing"
    | "tampering"
    | "repudiation"
    | "informationDisclosure"
    | "denialOfService"
    | "elevationOfPrivilege"

export const cards: Record<SuitKey, Record<string, Card>> = {
    spoofing: {
        four: {
            cardNumber: 3,
            suit: "Spoofing",
            value: "Four",
            text: "Lorem ipsum",
            example: "Lorem ipsum",
            mitigation: "Lorem ipsum",
        },
    },
    tampering: {
        four: {
            cardNumber: 15,
            suit: "Tampering",
            value: "Four",
            text: "Lorem ipsum",
            example: "Lorem ipsum",
            mitigation: "Lorem ipsum",
        },
        seven: {
            cardNumber: 18,
            suit: "Tampering",
            value: "Seven",
            text: "Lorem ipsum",
            example: "Lorem ipsum",
            mitigation: "Lorem ipsum",
        },
        nine: {
            cardNumber: 20,
            suit: "Tampering",
            value: "Nine",
            text: "Lorem ipsum",
            example: "Lorem ipsum",
            mitigation: "Lorem ipsum",
        },
    },
    repudiation: {
        king: {
            cardNumber: 37,
            suit: "Repudiation",
            value: "King",
            text: "Lorem ipsum",
            example: "Lorem ipsum",
            mitigation: "Lorem ipsum",
        },
    },
    informationDisclosure: {
        jack: {
            cardNumber: 48,
            suit: "InformationDisclosure",
            value: "Jack",
            text: "Lorem ipsum",
            example: "Lorem ipsum",
            mitigation: "Lorem ipsum",
        },
    },
    denialOfService: {
        two: {
            cardNumber: 52,
            suit: "DenialOfService",
            value: "Two",
            text: "Lorem ipsum",
            example: "Lorem ipsum",
            mitigation: "Lorem ipsum",
        },
        jack: {
            cardNumber: 61,
            suit: "DenialOfService",
            value: "Jack",
            text: "Lorem ipsum",
            example: "Lorem ipsum",
            mitigation: "Lorem ipsum",
        },
    },
    elevationOfPrivilege: {
        ace: {
            cardNumber: 74,
            suit: "ElevationOfPrivilege",
            value: "Ace",
            text: "Lorem ipsum",
            example: "Lorem ipsum",
            mitigation: "Lorem ipsum",
        },
    },
}

const tableCard1: UsersCard = {
    gameId: exampleGame.id,
    playerId: player2.id,
    card: cards.tampering.seven,
    location: "Table",
    threatLinked: true,
}

const tableCard2: UsersCard = {
    gameId: exampleGame.id,
    playerId: player3.id,
    card: cards.denialOfService.two,
    location: "Table",
    threatLinked: false,
}

export const exampleRound: Round = {
    state: exampleRoundState,
    hand: [
        cards.spoofing.four,
        cards.tampering.nine,
        cards.repudiation.king,
        cards.informationDisclosure.jack,
        cards.denialOfService.jack,
        cards.elevationOfPrivilege.ace,
    ],
    table: [tableCard1, tableCard2],
    playersScores: {
        [myId]: 5,
        [player2Id]: 4,
        [player3Id]: 3,
    },
}
