import {
    assignUserRoleAction,
    createGameAction,
    fetchGameInfoAction,
    fetchMembersAction,
    joinGameAction,
    kickUserAction,
    newParticipantAction,
    resetAssignRoleStatusAsction,
    resetCreateGameStatusAction,
    resetGameInfoStatusAction,
    resetJoinStatusAction,
    resetKickUserStatusAsction,
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
const assignRoleReducer = createReducer(assignUserRoleAction, resetAssignRoleStatusAsction).build()
const kickUserReducer = createReducer(kickUserAction, resetKickUserStatusAsction).build()

export const gamesReducer = combineReducers({
    createStatus: createGameReducer,
    fetchStatus: fetchGameInfoReducer,
    joinGame: joinGameReducer,
    members: membersReducer,
    assignRoleStatus: assignRoleReducer,
    kickUserStatus: kickUserReducer,
})
