import { PlayerTakesTrick } from "./../types/Events"
import { reducerWithInitialState } from "typescript-fsa-reducers"
import { createReducer } from "./../../../application/store/async/AsyncReducerCreator"
import { combineReducers } from "redux"
import {
    cardPlayedAction,
    fetchMatchStateAction,
    fetchScoresAction,
    nextPlayerAction,
    nextTurnAction,
    playerTakesTrickAction,
    putCardOnTableAction,
    resetFetchScoresStatusAction,
    resetMatchStateAction,
    resetPutCardOnTableStatusAction,
    resetUpdateCardOnTablesStatusAction,
    threatStatusAssignedAction,
    updateCardOnTableAction,
} from "./Actions"

const increasedScores = (currentScores: Record<string, number>, playerId: string) => {
    const increased = (currentScores[playerId] ?? 0) + 1
    const scores = { ...currentScores, [playerId]: increased }
    return scores
}

const fetchMatchStateReducer = createReducer(fetchMatchStateAction, resetMatchStateAction)
    .case(threatStatusAssignedAction, (state, action) => {
        if (state.params === action.gameId && state.data) {
            const newTable = state.data.table.map((card) =>
                card.card.cardNumber === action.cardNumber ? { ...card, threatLinked: action.newStatus } : card
            )
            return { ...state, data: { ...state.data, table: newTable } }
        } else {
            return state
        }
    })
    .case(nextPlayerAction, (state, action) => {
        if (state.params === action.gameId && state.data) {
            return {
                ...state,
                data: { ...state.data, state: { ...state.data.state, currentPlayer: action.newPlayer } },
            }
        } else {
            return state
        }
    })
    .case(cardPlayedAction, (state, action) => {
        if (state.params === action.gameId && state.data) {
            const newHand = state.data.hand.filter((card) => card.cardNumber !== action.card.cardNumber)
            const newTable = [...state.data.table, action]
            const newState = state.data.state.leadingSuit
                ? state.data.state
                : { ...state.data.state, leadingSuit: action.card.suit }
            return { ...state, data: { ...state.data, table: newTable, hand: newHand, state: newState } }
        } else {
            return state
        }
    })
    .case(nextTurnAction, (state, action) => {
        if (state.params === action.gameId && state.data) {
            return {
                ...state,
                data: {
                    ...state.data,
                    table: [],
                    state: { ...state.data.state, currentPlayer: action.player, leadingSuit: null },
                },
            }
        } else {
            return state
        }
    })
    .build()

const updateCardOnTableReducer = createReducer(updateCardOnTableAction, resetUpdateCardOnTablesStatusAction).build()
const putCardOnTableReducer = createReducer(putCardOnTableAction, resetPutCardOnTableStatusAction).build()
const playerTakesTrickReducer = reducerWithInitialState<PlayerTakesTrick | null>(null)
    .case(playerTakesTrickAction, (_, action) => action)
    .build()

const playerScoresReducer = reducerWithInitialState<Record<string, number>>({})
    .case(threatStatusAssignedAction, (state, action) => {
        if (action.newStatus === true) {
            return increasedScores(state, action.playerId)
        } else {
            return state
        }
    })
    .case(playerTakesTrickAction, (state, action) => {
        if (action.player) {
            return increasedScores(state, action.player)
        } else {
            return state
        }
    })
    .case(fetchMatchStateAction.done, (_, action) => action.result.playersScores)
    .case(fetchScoresAction.done, (_, action) => action.result)
    .build()

const fetchScoresReducer = createReducer(fetchScoresAction, resetFetchScoresStatusAction).build()

export const matchReducer = combineReducers({
    matchState: fetchMatchStateReducer,
    updateCardOnTable: updateCardOnTableReducer,
    putCardOnTable: putCardOnTableReducer,
    playerTakesTrick: playerTakesTrickReducer,
    scores: playerScoresReducer,
    fetchScores: fetchScoresReducer,
})
