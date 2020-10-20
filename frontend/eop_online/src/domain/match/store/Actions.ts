import actionCreatorFactory from "typescript-fsa"
import { Round } from "../types/Round"

const actionCreator = actionCreatorFactory("Match")

export const fetchMatchStateAction = actionCreator.async<string, Round, Error>("FETCH_MATCH_STATE")
export const resetMatchStateAction = actionCreator("RESET_MATCH_STATE")
