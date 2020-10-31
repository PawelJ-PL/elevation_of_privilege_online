import { PlayerTakesTrick } from "./../types/Events"
import { reducerWithInitialState } from "typescript-fsa-reducers"
import { createReducer } from "./../../../application/store/async/AsyncReducerCreator"
import { combineReducers } from "redux"
import {
    cardPlayedAction,
    fetchMatchStateAction,
    nextPlayerAction,
    nextTurnAction,
    playerTakesTrickAction,
    putCardOnTableAction,
    resetMatchStateAction,
    resetPutCardOnTableStatusAction,
    resetUpdateCardOnTablesStatusAction,
    threatStatusAssignedAction,
    updateCardOnTableAction,
} from "./Actions"

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

export const matchReducer = combineReducers({
    matchState: fetchMatchStateReducer,
    updateCardOnTable: updateCardOnTableReducer,
    putCardOnTable: putCardOnTableReducer,
    playerTakesTrick: playerTakesTrickReducer,
})
