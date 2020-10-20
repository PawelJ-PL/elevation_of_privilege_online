import client from "../../../application/api/BaseClient"
import { MatchNotFound } from "../types/Errors"
import { Round } from "../types/Round"

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
}
