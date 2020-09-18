import { ConnectedRouter } from "connected-react-router"
import React from "react"
import { Provider } from "react-redux"
import AppRouter from "./application/components/router/AppRouters"
import store, { history } from "./application/store"

function App() {
    return (
        <Provider store={store}>
            <ConnectedRouter history={history}>
                <AppRouter />
            </ConnectedRouter>
        </Provider>
    )
}

export default App
