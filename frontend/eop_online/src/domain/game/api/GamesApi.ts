import { GameAlreadyStarted, GameNotFound, UserAlreadyJoined, UserNotAccepted } from "./../types/Errors"
import { Game } from "./../types/Game"
import client from "../../../application/api/BaseClient"
import { UserIsNotGameMember } from "../types/Errors"
import { Member, MemberRole } from "../types/Member"

export default {
    createGame(ownerNickname: string, description?: string | null): Promise<Game> {
        return client
            .post<Game>("/games", { ownerNickname, description })
            .then((resp) => resp.data)
    },
    getGameInfo(gameId: string): Promise<Game | null> {
        return client
            .get<Game>("/games/" + gameId)
            .then((resp) => resp.data)
            .catch((err) => {
                if (err.response?.status === 404) {
                    return null
                } else if (err.response?.status === 403 && err.response?.data?.reason === "NotAMember") {
                    return Promise.reject(new UserIsNotGameMember(err.response?.message ?? "User is not game member"))
                } else if (err.response?.status === 403 && err.response?.data?.reason === "NotAccepted") {
                    return Promise.reject(new UserNotAccepted(err.response?.message ?? "User not accepted"))
                } else {
                    return Promise.reject(err)
                }
            })
    },
    joinGame(gameId: string, nickname: string): Promise<void> {
        return client
            .put<void>("/games/" + gameId, { nickname })
            .then((resp) => resp.data)
            .catch((err) => {
                if (err.response?.status === 404) {
                    return Promise.reject(new GameNotFound(`Game ${gameId} not found`))
                } else if (err.response?.status === 409) {
                    return Promise.reject(new UserAlreadyJoined())
                } else if (err.response?.status === 412 && err.response?.data?.reason === "GameAlreadyStarted") {
                    return Promise.reject(new GameAlreadyStarted(`Game ${gameId} already started`))
                } else {
                    return Promise.reject(err)
                }
            })
    },
    fetchMembers(gameId: string): Promise<Member[]> {
        return client.get<Member[]>(`/games/${gameId}/members`).then((resp) => resp.data)
    },
    assignRole(gameId: string, participantId: string, role: MemberRole): Promise<void> {
        return client
            .put<void>(`/games/${gameId}/members/${participantId}/roles/${role}`)
            .then((resp) => resp.data)
            .catch((err) => {
                if (err.response?.status === 404) {
                    return Promise.reject(new GameNotFound(`Game ${gameId} not found`))
                } else if (err.response?.status === 412 && err.response?.data?.reason === "GameAlreadyStarted") {
                    return Promise.reject(new GameAlreadyStarted(`Game ${gameId} already started`))
                } else {
                    return Promise.reject(err)
                }
            })
    },
    kickUser(gameId: string, participantId: string): Promise<void> {
        return client
            .delete<void>(`/games/${gameId}/members/${participantId}`)
            .then((resp) => resp.data)
            .catch((err) => {
                if (err.response?.status === 404) {
                    return Promise.reject(new GameNotFound(`Game ${gameId} not found`))
                } else if (err.response?.status === 412 && err.response?.data?.reason === "GameAlreadyStarted") {
                    return Promise.reject(new GameAlreadyStarted(`Game ${gameId} already started`))
                } else {
                    return Promise.reject(err)
                }
            })
    },
}
