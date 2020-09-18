import { UserRoleChanged, UserRemoved, NewParticipant } from "./../types/Events"
import { Game } from "./../types/Game"
import actionCreatorFactory from "typescript-fsa"
import { Member, MemberRole } from "../types/Member"

const actionCreator = actionCreatorFactory("Game")

export const createGameAction = actionCreator.async<
    { ownerNickname: string; description?: string | null },
    Game,
    Error
>("CREATE_GAME")
export const resetCreateGameStatusAction = actionCreator("RESET_CREATE_GAME_STATUS")
export const fetchGameInfoAction = actionCreator.async<string, Game | null, Error>("FETCH_GAME_INFO")
export const resetGameInfoStatusAction = actionCreator("RESET_GAME_INFO_STATUS")
export const joinGameAction = actionCreator.async<{ gameId: string; nickname: string }, void, Error>("JOIN_GAME")
export const resetJoinStatusAction = actionCreator("RESET_JOIN_STATUS")
export const fetchMembersAction = actionCreator.async<string, Member[], Error>("FETCH_MEMBERS")
export const assignUserRoleAction = actionCreator.async<
    { gameId: string; participantId: string; role: MemberRole },
    void,
    Error
>("ASSIGN_USER_ROLE")
export const resetAssignRoleStatusAsction = actionCreator("RESET_ASSIGN_ROLE_STATUS")
export const kickUserAction = actionCreator.async<{ gameId: string; participantId: string }, void, Error>("KICK_USER")
export const resetKickUserStatusAsction = actionCreator("RESET_KICK_USER_STATUS")
export const userRoleChangedAction = actionCreator<UserRoleChanged>("USER_ROLE_CHANGED")
export const userRemovedAction = actionCreator<UserRemoved>("USER_REMOVED")
export const newParticipantAction = actionCreator<NewParticipant>("NEW_PARTICIPANT")
