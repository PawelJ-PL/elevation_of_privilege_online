import { render } from "@testing-library/react"
import { createLocation, createMemoryHistory } from "history"
import React from "react"
import { Router } from "react-router-dom"
import { OperationStatus } from "../../../application/store/async/AsyncOperationResult"
import { UserIsNotGameMember, UserNotAccepted, UserRemoved } from "../types/Errors"
import { Game } from "../types/Game"
import { SingleGamePage } from "./SingleGamePage"
import * as H from "history"
import * as chakra from "@chakra-ui/react"
import { toastMock } from "../../../testutils/mocks/toastMock"

/* eslint-disable react/display-name */

jest.mock("./anteroom/AnteroomContainer", () => (props: { game: Game }) => (
    <div data-testid="ANTEROOM_MOCK">{JSON.stringify(props.game)}</div>
))

jest.mock("../../match/components/MatchContainer", () => (props: { game: Game }) => (
    <div data-testid="MATCH_MOCK">{JSON.stringify(props.game)}</div>
))

jest.mock("../../../application/components/common/PageLoader")

jest.mock("./JoinGameModal", () => (props: { isOpen: boolean; gameId: string }) => (
    <div data-testid="JOIN_MODAL_MOCK">
        <div>{props.gameId}</div>
        <div>{JSON.stringify(props.isOpen)}</div>
    </div>
))

jest.mock("../../../application/components/common/AlertBox")

jest.mock("./AfterGameSummary", () => (props: { game: Game }) => (
    <div data-testid="GAME_SUMMARY_MOCK">{JSON.stringify(props.game)}</div>
))

jest.mock("react-router-dom", () => ({
    ...jest.requireActual("react-router-dom"),
}))

const url = "/game/foo-bar"

const gameData = { id: "foo-bar", description: null, creator: "baz", startedAt: null, finishedAt: null }

const gameInfo = {
    status: OperationStatus.FINISHED,
    data: gameData,
    params: "foo-bar",
}

const defaultProps = {
    match: {
        params: { gameId: "foo-bar" },
        isExact: true,
        path: "/game/:gameId",
        url,
    },
    history: createMemoryHistory(),
    location: createLocation(url),
    fetchGameInfo: jest.fn(),
    resetGameStatus: jest.fn(),
    startAnteroomWs: jest.fn(),
    stopAnteroomWs: jest.fn(),
    gameInfo,
}

const routerWrapper = (history?: H.History) => {
    const wrapper: React.ComponentType = ({ children }) => (
        <Router history={history ?? createMemoryHistory()}>{children}</Router>
    )
    return wrapper
}

describe("Single game page", () => {
    describe("View selection", () => {
        it("should render anteroom when game data exists without start date", () => {
            const game = { ...gameInfo, data: { ...gameData, startedAt: null } }
            const props = { ...defaultProps, gameInfo: game }

            const element = render(<SingleGamePage {...props} />)
            const component = element.getByTestId("ANTEROOM_MOCK")
            expect(component.textContent).toEqual(JSON.stringify(game.data))
        })

        it("should render match view if game data exists with start date and without finish date", () => {
            const game = { ...gameInfo, data: { ...gameData, startedAt: "2020-11-21T16:04:10Z", finishedAt: null } }
            const props = { ...defaultProps, gameInfo: game }

            const element = render(<SingleGamePage {...props} />)
            const component = element.getByTestId("MATCH_MOCK")
            expect(component.textContent).toEqual(JSON.stringify(game.data))
        })

        it("should render loader if current state contains different game", () => {
            const game = { ...gameInfo, params: "other-game" }
            const props = { ...defaultProps, gameInfo: game }

            const element = render(<SingleGamePage {...props} />)
            const component = element.getByTestId("LOADER_MOCK")
            expect(component.textContent).toEqual("Loading game")
        })

        it("should render loader when waiting for approval", () => {
            const game = { ...gameInfo, error: new UserNotAccepted("User not accepted") }
            const props = { ...defaultProps, gameInfo: game }

            const element = render(<SingleGamePage {...props} />)
            const component = element.getByTestId("LOADER_MOCK")
            expect(component.textContent).toEqual("Waiting for approval")
        })

        it("should render join modal if user not a member", () => {
            const game = { ...gameInfo, error: new UserIsNotGameMember("User not game member") }
            const props = { ...defaultProps, gameInfo: game }

            const element = render(<SingleGamePage {...props} />)
            const component = element.getByTestId("JOIN_MODAL_MOCK")
            expect(component.firstChild?.textContent).toEqual(gameInfo.params)
            expect(component.lastChild?.textContent).toEqual("true")
        })

        it("should render alert on error", () => {
            const game = { ...gameInfo, error: new Error("Some error") }
            const props = { ...defaultProps, gameInfo: game }

            const element = render(<SingleGamePage {...props} />)
            const component = element.getByTestId("ALERT_MOCK")
            const textContents = Array.from(component.children).map((c) => c.textContent)
            expect(textContents).toEqual(["Unable to load game data", "Some error", "error"])
        })

        it("should render after game summary when finished date exists", () => {
            const game = {
                ...gameInfo,
                data: { ...gameData, startedAt: "2020-11-21T16:04:10Z", finishedAt: "2020-11-21T17:04:10Z" },
            }
            const props = { ...defaultProps, gameInfo: game }

            const element = render(<SingleGamePage {...props} />)
            const component = element.getByTestId("GAME_SUMMARY_MOCK")
            expect(component.textContent).toEqual(JSON.stringify(game.data))
        })
    })

    describe("Effect actions", () => {
        it("should fetch game info on mount", () => {
            const fetchGameInfo = jest.fn()
            const props = { ...defaultProps, fetchGameInfo }

            render(<SingleGamePage {...props} />)

            expect(fetchGameInfo).toHaveBeenCalledWith(gameData.id)
        })

        describe("Redirect", () => {
            it("should redirect to home page if game not found", () => {
                const history = createMemoryHistory({ initialEntries: [url], initialIndex: 0 })
                const toast = jest.fn()
                const useToastMock = jest.spyOn(chakra, "useToast").mockImplementation(() => toastMock(toast))

                const game = { ...gameInfo, data: null }
                const props = { ...defaultProps, gameInfo: game, history }

                render(<SingleGamePage {...props} />, { wrapper: routerWrapper(history) })
                expect(history.entries).toHaveLength(2)
                expect(history.location.pathname).toEqual("/")
                expect(toast).toHaveBeenCalledTimes(1)
                expect(toast).toHaveBeenCalledWith({
                    duration: 3000,
                    position: "top",
                    status: "warning",
                    title: "Game not found",
                })

                useToastMock.mockRestore()
            })

            it("should redirect to home page user was removed", () => {
                const history = createMemoryHistory({ initialEntries: [url], initialIndex: 0 })
                const toast = jest.fn()
                const useToastMock = jest.spyOn(chakra, "useToast").mockImplementation(() => toastMock(toast))

                const game = { ...gameInfo, error: new UserRemoved("User was removed") }
                const props = { ...defaultProps, gameInfo: game, history }

                render(<SingleGamePage {...props} />, { wrapper: routerWrapper(history) })
                expect(history.entries).toHaveLength(2)
                expect(history.location.pathname).toEqual("/")
                expect(toast).toHaveBeenCalledTimes(1)
                expect(toast).toHaveBeenCalledWith({
                    duration: 3000,
                    position: "top",
                    status: "warning",
                    title: "User removed from game",
                })

                useToastMock.mockRestore()
            })
        })

        describe("Start websocket", () => {
            it("Should start websocket when waiting for approval", () => {
                const game = { ...gameInfo, error: new UserNotAccepted("User not accepted") }
                const startWsMock = jest.fn()
                const props = { ...defaultProps, gameInfo: game, startAnteroomWs: startWsMock }

                render(<SingleGamePage {...props} />)

                expect(startWsMock).toHaveBeenCalledTimes(1)
                expect(startWsMock).toHaveBeenCalledWith("foo-bar")
            })

            it("Should start websocket when game exists without start date", () => {
                const game = { ...gameInfo, data: { ...gameData, startedAt: null } }
                const startWsMock = jest.fn()
                const props = { ...defaultProps, gameInfo: game, startAnteroomWs: startWsMock }

                render(<SingleGamePage {...props} />)

                expect(startWsMock).toHaveBeenCalledTimes(1)
                expect(startWsMock).toHaveBeenCalledWith("foo-bar")
            })
        })

        describe("Stop websocket", () => {
            it("Should stop websocket when game exists with start date", () => {
                const game = { ...gameInfo, data: { ...gameData, startedAt: "2020-11-21T16:04:10Z" } }
                const stopWsMock = jest.fn()
                const props = { ...defaultProps, gameInfo: game, stopAnteroomWs: stopWsMock }

                render(<SingleGamePage {...props} />)

                expect(stopWsMock).toHaveBeenCalledTimes(1)
                expect(stopWsMock).toHaveBeenCalledWith("foo-bar")
            })

            it("Should stop websocket on unmount", () => {
                const stopWsMock = jest.fn()
                const props = { ...defaultProps, stopAnteroomWs: stopWsMock }

                const element = render(<SingleGamePage {...props} />)
                element.unmount()

                expect(stopWsMock).toHaveBeenCalledTimes(1)
                expect(stopWsMock).toHaveBeenCalledWith("foo-bar")
            })
        })

        describe("Reset game status", () => {
            it("should reset status on unmount", () => {
                const resetStatusMock = jest.fn()
                const props = { ...defaultProps, resetGameStatus: resetStatusMock }

                const element = render(<SingleGamePage {...props} />)
                element.unmount()

                expect(resetStatusMock).toHaveBeenCalledTimes(1)
            })
        })
    })
})
