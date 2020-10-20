import { createReducer } from "./../../../application/store/async/AsyncReducerCreator"
import { combineReducers } from "redux"
import { fetchMatchStateAction, resetMatchStateAction } from "./Actions"

const fetchMatchStateReducer = createReducer(fetchMatchStateAction, resetMatchStateAction).build()

export const matchReducer = combineReducers({
    matchState: fetchMatchStateReducer,
})
