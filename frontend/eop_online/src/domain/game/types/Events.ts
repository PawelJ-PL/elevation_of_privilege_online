import { MemberRole } from "./Member"

export type UserRoleChanged = { gameId: string; userId: string; role: MemberRole }

export type UserRemoved = { gameId: string; userId: string }

export type NewParticipant = { gameId: string; userId: string; nickName: string }

export type GameStarted = { gameId: string }
