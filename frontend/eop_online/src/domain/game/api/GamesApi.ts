import { UserGameSummary } from "./../types/UserGameSummary"
import {
    GameAlreadyFinished,
    GameAlreadyStarted,
    GameNotFound,
    NotEnoughPlayers,
    TooManyPlayers,
    UserAlreadyJoined,
    UserIsNotGameOwner,
    UserNotAccepted,
} from "./../types/Errors"
import { Game } from "./../types/Game"
import client from "../../../application/api/BaseClient"
import { UserIsNotGameMember } from "../types/Errors"
import { Member, MemberRole } from "../types/Member"

const handlePreconditionFailed = (reason?: string, message?: string) => {
    switch (reason) {
        case "GameAlreadyStarted":
            return Promise.reject(new GameAlreadyStarted(message ?? `Game already started`))
        case "GameAlreadyFinished":
            return Promise.reject(new GameAlreadyFinished(message ?? "Game already finished"))
        case "NotEnoughPlayers":
            return Promise.reject(new NotEnoughPlayers(message ?? "Not enough players"))
        case "TooManyPlayers":
            return Promise.reject(new TooManyPlayers(message ?? "Too many players"))
        default:
            return undefined
    }
}

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
                    return Promise.reject(
                        new UserIsNotGameMember(err.response?.data?.message ?? "User is not game member")
                    )
                } else if (err.response?.status === 403 && err.response?.data?.reason === "NotAccepted") {
                    return Promise.reject(new UserNotAccepted(err.response?.data?.message ?? "User not accepted"))
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
                } else if (err.response?.status === 412) {
                    return (
                        handlePreconditionFailed(err.response.data?.message, err.response.data?.message) ??
                        Promise.reject(err)
                    )
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
                } else if (err.response?.status === 412) {
                    return (
                        handlePreconditionFailed(err.response.data?.message, err.response.data?.message) ??
                        Promise.reject(err)
                    )
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
                } else if (err.response?.status === 412) {
                    return (
                        handlePreconditionFailed(err.response.data?.message, err.response.data?.message) ??
                        Promise.reject(err)
                    )
                } else {
                    return Promise.reject(err)
                }
            })
    },

    startGame(gameId: string): Promise<void> {
        return client
            .post<void>(`/games/${gameId}`)
            .then((resp) => resp.data)
            .catch((err) => {
                if (err.response?.status === 404) {
                    return Promise.reject(new GameNotFound(`Game ${gameId} not found`))
                } else if (err.response?.status === 403) {
                    return Promise.reject(new UserIsNotGameOwner("User is not a game owner"))
                } else if (err.response?.status === 412) {
                    return (
                        handlePreconditionFailed(err.response.data?.message, err.response.data?.message) ??
                        Promise.reject(err)
                    )
                } else {
                    return Promise.reject(err)
                }
            })
    },

    fetchAvailableGames(): Promise<UserGameSummary[]> {
        return client.get<UserGameSummary[]>("/games").then((resp) => resp.data)
    },

    deleteGame(gameId: string): Promise<void> {
        return client.delete<void>(`/games/${gameId}`).then((resp) => resp.data)
    },
}
