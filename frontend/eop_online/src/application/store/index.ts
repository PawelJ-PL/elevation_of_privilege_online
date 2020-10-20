import { matchesEpics } from './../../domain/match/store/Epics';
import { matchReducer } from './../../domain/match/store/Reducers';
import { usersEpics } from "./../../domain/user/store/Epics"
import { usersReducer } from "./../../domain/user/store/Reducers"
import { gamesEpics } from "./../../domain/game/store/Epics"
import { gamesReducer } from "./../../domain/game/store/Reducers"
import { Action, applyMiddleware, combineReducers, compose, createStore } from "redux"
import { combineEpics, createEpicMiddleware } from "redux-observable"
import { connectRouter, routerMiddleware } from "connected-react-router"
import { createBrowserHistory } from "history"

export const history = createBrowserHistory({ basename: process.env.REACT_APP_BASE_PATH })

const rootReducer = combineReducers({
    router: connectRouter(history),
    games: gamesReducer,
    users: usersReducer,
    matches: matchReducer
})

export type AppState = ReturnType<typeof rootReducer>

const rootEpic = combineEpics(gamesEpics, usersEpics, matchesEpics)

function configure() {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const composeEnhancer: typeof compose = (window as any).__REDUX_DEVTOOLS_EXTENSION_COMPOSE__ || compose
    const epicMiddleware = createEpicMiddleware<Action, Action, AppState>()

    const store = createStore(rootReducer, composeEnhancer(applyMiddleware(routerMiddleware(history), epicMiddleware)))

    epicMiddleware.run(rootEpic)
    return store
}

const applicationStore = configure()

export default applicationStore
