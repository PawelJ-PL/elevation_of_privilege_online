import {
    CardNotFound,
    CardNotOnTable,
    CardNotOnTheHand,
    OtherPlayersCard,
    OtherPlayersTurn,
    PlayerAlreadyPlayedCard,
    SuitNotMatch,
    ThreatStatusAlreadyAssigned,
} from "./../types/Errors"
import { UpdateCardOnTableData } from "./../types/UpdateCardOnTableData"
import client from "../../../application/api/BaseClient"
import { MatchNotFound, NotAPlayer } from "../types/Errors"
import { Round } from "../types/Round"

const handleCommonForbiddenResponses = (reason: string, message?: string) => {
    switch (reason) {
        case "NotAPlayer":
            return Promise.reject(new NotAPlayer(message ?? reason))
        case "OtherPlayersTurn":
            return Promise.reject(new OtherPlayersTurn(message ?? reason))
        case "OtherPlayersCard":
            return Promise.reject(new OtherPlayersCard(message ?? reason))
        default:
            return undefined
    }
}

const handleCommonPreconditionFailedResponses = (reason: string, message?: string) => {
    switch (reason) {
        case "Card not found":
            return Promise.reject(new CardNotFound(message ?? reason))
        default:
            return undefined
    }
}

const handleUpdateCardOnTablePreconditionFailedResponses = (reason: string, message?: string) => {
    const commonResponse = handleCommonPreconditionFailedResponses(reason, message)
    if (commonResponse) {
        return commonResponse
    } else {
        switch (reason) {
            case "CardNotOnTable":
                return Promise.reject(new CardNotOnTable(message ?? reason))
            case "ThreatStatusAlreadyAssigned":
                return Promise.reject(new ThreatStatusAlreadyAssigned(message ?? reason))
            default:
                return undefined
        }
    }
}

const handlePutCardPreconditionFailedResponses = (reason: string, message?: string) => {
    const commonResponse = handleCommonPreconditionFailedResponses(reason, message)
    if (commonResponse) {
        return commonResponse
    } else {
        switch (reason) {
            case "CardNotOnTheHand":
                return Promise.reject(new CardNotOnTheHand(message ?? reason))
            case "SuitNotMatch":
                return Promise.reject(new SuitNotMatch(message ?? reason))
            case "PlayerAlreadyPlayedCard":
                return Promise.reject(new PlayerAlreadyPlayedCard(message ?? reason))
            default:
                return undefined
        }
    }
}

export default {
    fetchMatchState(matchId: string): Promise<Round> {
        return client
            .get<Round>(`/matches/${matchId}`)
            .then((resp) => resp.data)
            .catch((err) => {
                if (err.response?.status === 404) {
                    return Promise.reject(new MatchNotFound(`Match ${matchId} not found`))
                } else {
                    return Promise.reject(err)
                }
            })
    },

    updateCardOnTable(matchId: string, cardNumber: number, data: UpdateCardOnTableData): Promise<void> {
        return client
            .patch(`/matches/${matchId}/table/cards/${cardNumber}`, data)
            .then(() => void 0)
            .catch((err) => {
                if (err.response?.status === 404) {
                    return Promise.reject(new MatchNotFound(`Match ${matchId} not found`))
                } else if (err.response?.status === 403 && err.response.data?.reason) {
                    return (
                        handleCommonForbiddenResponses(err.response.data.reason, err.response.data.message) ??
                        Promise.reject(err)
                    )
                } else if (err.response?.status === 412 && err.response.data?.reason) {
                    return (
                        handleUpdateCardOnTablePreconditionFailedResponses(
                            err.response.data.reason,
                            err.response.data.message
                        ) ?? Promise.reject(err)
                    )
                } else {
                    return Promise.reject(err)
                }
            })
    },

    putCardOnTable(matchId: string, cardNumber: number): Promise<void> {
        return client
            .put(`/matches/${matchId}/table/cards/${cardNumber}`)
            .then(() => void 0)
            .catch((err) => {
                if (err.response?.status === 404) {
                    return Promise.reject(new MatchNotFound(`Match ${matchId} not found`))
                } else if (err.response?.status === 403 && err.response.data?.reason) {
                    return (
                        handleCommonForbiddenResponses(err.response.data.reason, err.response.data.message) ??
                        Promise.reject(err)
                    )
                } else if (err.response?.status === 412 && err.response.data?.reason) {
                    return (
                        handlePutCardPreconditionFailedResponses(err.response.data.reason, err.response.data.message) ??
                        Promise.reject(err)
                    )
                } else {
                    return Promise.reject(err)
                }
            })
    },
    getScores(matchId: string): Promise<Record<string, number>> {
        return client.get<Promise<Record<string, number>>>(`/matches/${matchId}/scores`).then((resp) => resp.data)
    },
}
