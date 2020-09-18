import { combineReducers } from 'redux';
import { fetchMeInfoAction } from "./Actions"
import { createReducer } from "./../../../application/store/async/AsyncReducerCreator"

const userInfoReducer = createReducer(fetchMeInfoAction)

export const usersReducer = combineReducers({
    current: userInfoReducer
})