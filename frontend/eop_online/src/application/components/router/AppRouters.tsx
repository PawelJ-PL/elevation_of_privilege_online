import React from "react"
import { Route, Switch } from "react-router-dom"
import AlertBox from "../common/AlertBox"
import { routes } from "./Routes"

const PageNotFound = () => (
    <AlertBox
        title="Page not found"
        description="Unable to find requested page"
        status="error"
        containerProps={{ marginTop: "0.5em", marginLeft: ["0", "0", "10%"], width: ["100vw", "100vw", "80vw"] }}
    />
)

const AppRouter: React.FC = () => {
    return (
        <Switch>
            {routes
                .map((route) => {
                    return (
                        <Route
                            key={route.path}
                            exact={route.exact}
                            path={route.path}
                            component={route.component}
                            render={route.render}
                        />
                    )
                })
                .concat(<Route key={routes.length} render={() => <PageNotFound />} />)}
        </Switch>
    )
}

export default AppRouter
