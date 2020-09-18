import { Session } from "./../types/Session"
import { createEpic } from "../../../application/store/async/AsyncActionErrorCreator"
import { fetchMeInfoAction } from "./Actions"
import UsersApi from "../api/UsersApi"
import { combineEpics } from "redux-observable"

const fetchMeInfoEpic = createEpic<void, Session, Error>(fetchMeInfoAction, () => UsersApi.fetchMeInfo())

export const usersEpics = combineEpics(fetchMeInfoEpic)
