import { Session } from "./../types/Session"
import actionCreatorFactory from "typescript-fsa"

const actionCreator = actionCreatorFactory("User")

export const fetchMeInfoAction = actionCreator.async<void, Session, Error>("FETCH_ME_INFO")
