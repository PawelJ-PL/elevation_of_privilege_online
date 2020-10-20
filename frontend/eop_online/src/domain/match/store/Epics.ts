import { combineEpics } from "redux-observable"
import { createEpic } from "../../../application/store/async/AsyncActionErrorCreator"
import MatchesApi from "../api/MatchesApi"
import { Round } from "../types/Round"
import { fetchMatchStateAction } from "./Actions"

const fetchMatchStateEpic = createEpic<string, Round, Error>(fetchMatchStateAction, (params) =>
    MatchesApi.fetchMatchState(params)
)

export const matchesEpics = combineEpics(fetchMatchStateEpic)
