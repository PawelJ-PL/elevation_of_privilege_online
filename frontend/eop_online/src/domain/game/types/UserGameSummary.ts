import { MemberRole } from "./Member"
export type UserGameSummary = {
    id: string
    description?: string | null
    playerNickname: string
    ownerNickname: string
    isOwner: boolean
    currentUserRole?: MemberRole | null
    startedAt?: string | null
    finishedAt?: string | null
}
