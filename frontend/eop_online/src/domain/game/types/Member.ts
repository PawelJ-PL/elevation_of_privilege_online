export type Member = {
    id: string
    nickname: string
    role?: null | MemberRole
}

export type MemberRole = "Player" | "Observer"