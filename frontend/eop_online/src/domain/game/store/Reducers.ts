import {
    assignUserRoleAction,
    createGameAction,
    deleteGameAction,
    fetchGameInfoAction,
    fetchMembersAction,
    fetchUserGamesAction,
    joinGameAction,
    kickUserAction,
    newParticipantAction,
    resetAssignRoleStatusAction,
    resetCreateGameStatusAction,
    resetDeleteGameStatusAction,
    resetGameInfoStatusAction,
    resetJoinStatusAction,
    resetKickUserStatusAction,
    resetStartGameStatusAction,
    resetUserGamesInfoAction,
    startGameAction,
    userRemovedAction,
    userRoleChangedAction,
} from "./Actions"
import { createReducer } from "./../../../application/store/async/AsyncReducerCreator"
import { combineReducers } from "redux"
import { OperationStatus } from "../../../application/store/async/AsyncOperationResult"
import { Member } from "../types/Member"

const createGameReducer = createReducer(createGameAction, resetCreateGameStatusAction).build()
const fetchGameInfoReducer = createReducer(fetchGameInfoAction, resetGameInfoStatusAction).build()
const joinGameReducer = createReducer(joinGameAction, resetJoinStatusAction).build()
const membersReducer = createReducer(fetchMembersAction)
    .case(userRemovedAction, (state, action) => {
        if (state.status === OperationStatus.FINISHED && state.params === action.gameId && state.data) {
            const updatedMembers = state.data.filter((member) => member.id !== action.userId)
            return {
                ...state,
                data: updatedMembers,
            }
        } else {
            return state
        }
    })
    .case(newParticipantAction, (state, action) => {
        if (state.status === OperationStatus.FINISHED && state.params === action.gameId && state.data) {
            const newMember: Member = { id: action.userId, nickname: action.nickName }
            const updatedMembers = [...state.data, newMember]
            return {
                ...state,
                data: updatedMembers,
            }
        } else {
            return state
        }
    })
    .case(userRoleChangedAction, (state, action) => {
        if (state.status === OperationStatus.FINISHED && state.params === action.gameId && state.data) {
            const updatedMembers = state.data.map((member) => {
                if (member.id === action.userId) {
                    return { ...member, role: action.role }
                } else {
                    return member
                }
            })
            return {
                ...state,
                data: updatedMembers,
            }
        } else {
            return state
        }
    })
    .build()
const assignRoleReducer = createReducer(assignUserRoleAction, resetAssignRoleStatusAction).build()
const kickUserReducer = createReducer(kickUserAction, resetKickUserStatusAction).build()
const startGameReducer = createReducer(startGameAction, resetStartGameStatusAction).build()
const userGamesReducer = createReducer(fetchUserGamesAction, resetUserGamesInfoAction)
    .case(deleteGameAction.done, (state, action) => {
        if (state.status !== OperationStatus.FINISHED || !state.data) {
            return state
        } else {
            const updatedGames = state.data.filter((game) => game.id !== action.params)
            return { ...state, data: updatedGames }
        }
    })
    .build()
const deleteGameReducer = createReducer(deleteGameAction, resetDeleteGameStatusAction).build()

export const gamesReducer = combineReducers({
    createStatus: createGameReducer,
    fetchStatus: fetchGameInfoReducer,
    joinGame: joinGameReducer,
    members: membersReducer,
    assignRoleStatus: assignRoleReducer,
    kickUserStatus: kickUserReducer,
    startGame: startGameReducer,
    allGames: userGamesReducer,
    deleteGameStatus: deleteGameReducer,
})
