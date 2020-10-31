import { CardPlayed, GameFinished, NextPlayer, NextTurn, PlayerTakesTrick } from "./../types/Events"
import { UpdateCardOnTableData } from "./../types/UpdateCardOnTableData"
import actionCreatorFactory from "typescript-fsa"
import { Round } from "../types/Round"
import { ThreatStatusAssigned } from "../types/Events"

const actionCreator = actionCreatorFactory("Match")

export type UpdateCardPayload = {
    matchId: string
    cardNumber: number
    data: UpdateCardOnTableData
}

export type PutCardPayload = {
    matchId: string
    cardNumber: number
}

export const fetchMatchStateAction = actionCreator.async<string, Round, Error>("FETCH_MATCH_STATE")
export const resetMatchStateAction = actionCreator("RESET_MATCH_STATE")
export const updateCardOnTableAction = actionCreator.async<UpdateCardPayload, void, Error>("UPDATE_CARD_ON_TABLE")
export const resetUpdateCardOnTablesStatusAction = actionCreator("RESET_UPDATE_CARD_ON_TABLE_STATUS")
export const putCardOnTableAction = actionCreator.async<PutCardPayload, void, Error>("PYT_CARD_ON_TABLE")
export const resetPutCardOnTableStatusAction = actionCreator("RESET_PUT_CARD_ON_TABLE_STATUS")
export const threatStatusAssignedAction = actionCreator<ThreatStatusAssigned>("THREAT_STATUS_ASSIGNED")
export const nextPlayerAction = actionCreator<NextPlayer>("NEXT_PLAYER")
export const cardPlayedAction = actionCreator<CardPlayed>("CARD_PLAYED")
export const nextTurnAction = actionCreator<NextTurn>("NEXT_TURN")
export const gameFinishedAction = actionCreator<GameFinished>("GAME_FINISHED")
export const playerTakesTrickAction = actionCreator<PlayerTakesTrick>("PLAYER_TAKES_TRICK")
