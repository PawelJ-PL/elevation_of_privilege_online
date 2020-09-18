/* eslint-disable react/display-name */
/* eslint-disable @typescript-eslint/no-explicit-any */
import { RouteComponentProps } from "react-router-dom"
import React from "react"
import MainPage from "../pages/main/MainPage"
import SingleGamePage from "../../../domain/game/components/SingleGamePage"

type Route = {
    path: string
    exact: boolean
    component?: React.ComponentType<RouteComponentProps<any>> | React.ComponentType<any>
    render?: (props: RouteComponentProps<any>) => React.ReactNode
}

export const routes: Route[] = [
    {
        path: "/",
        exact: true,
        component: MainPage,
    },
    {
        path: "/game/:gameId",
        exact: true,
        render: (props) => <SingleGamePage key={props.match.params.gameId} />,
    },
]
