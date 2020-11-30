import { Card } from "../../../domain/match/types/Card"
import { Round } from "../../../domain/match/types/Round"
import { UsersCard } from "../../../domain/match/types/UsersCard"
import { scores } from "../game"
import { RoundState } from "./../../../domain/match/types/RoundState"

const roundState: RoundState = {
    gameId: "foo-bar",
    currentPlayer: "111",
    leadingSuit: "InformationDisclosure",
}

export const card1: Card = {
    cardNumber: 7,
    suit: "Spoofing",
    value: "eight",
    text: "Lorem ipsum",
    example: "Lorem ipsum",
    mitigation: "Lorem",
}

export const card2: Card = {
    cardNumber: 39,
    suit: "InformationDisclosure",
    value: "two",
    text: "Lorem ipsum",
    example: "Lorem ipsum",
    mitigation: "Lorem ipsum",
}

export const usersCard: UsersCard = {
    gameId: "foo-bar",
    playerId: "222",
    card: card2,
    location: "Table",
}

export const round: Round = {
    state: roundState,
    hand: [card1],
    table: [usersCard],
    playersScores: scores,
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
        seven: {
            cardNumber: 18,
            suit: "Tampering",
            value: "Seven",
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
