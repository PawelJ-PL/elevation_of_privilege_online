import { render } from "@testing-library/react"
import React from "react"
import { OperationStatus } from "../../../application/store/async/AsyncOperationResult"
import { members, scores } from "../../../testutils/constants/game"
import { round } from "../../../testutils/constants/match"
import { Game } from "../../game/types/Game"
import { Member } from "../../game/types/Member"
import { Session } from "../../user/types/Session"
import { Round } from "../types/Round"
import { MatchContainer } from "./MatchContainer"
import * as chakra from "@chakra-ui/react"
import { toastMock } from "../../../testutils/mocks/toastMock"

/* eslint-disable react/display-name */

jest.mock("../../../application/components/common/AlertBox")

jest.mock(
    "./MatchView",
    () => (props: { game: Game; round: Round; user: Session; members: Member[]; scores: Record<string, number> }) => (
        <div data-testid="MATCH_VIEW_MOCK">
            <div>{JSON.stringify(props.game)}</div>
            <div>{JSON.stringify(props.round)}</div>
            <div>{JSON.stringify(props.user)}</div>
            <div>{JSON.stringify(props.members)}</div>
            <div>{JSON.stringify(props.scores)}</div>
        </div>
    )
)

const game = { id: "foo-bar", description: null, creator: "baz", startedAt: null, finishedAt: null }
const session = { userId: "111", createdAt: "2020-11-23T23:12:00Z" }

const matchData = { status: OperationStatus.FINISHED, data: round, params: "foo-bar" }
const sessionData = { status: OperationStatus.FINISHED, data: session }
const membersData = { status: OperationStatus.FINISHED, data: members, params: "foo-bart" }

const defaultProps = {
    game,
    matchState: matchData,
    session: sessionData,
    members: membersData,
    playerTakesTrick: null,
    scores,
    fetchMatchState: jest.fn(),
    resetMatchState: jest.fn(),
    fetchCurrentUser: jest.fn(),
    fetchGameMembers: jest.fn(),
    connectMatchWebSocket: jest.fn(),
    disconnectMatchWebSocket: jest.fn(),
}

describe("Match container", () => {
    describe("View selection", () => {
        it("should render error message if loading match failed", () => {
            const props = {
                ...defaultProps,
                matchState: { status: OperationStatus.FAILED, error: new Error("Some error") },
            }
            const element = render(<MatchContainer {...props} />)
            const component = element.getByTestId("ALERT_MOCK")
            const textContents = Array.from(component.children).map((c) => c.textContent)
            expect(textContents).toEqual(["Unable to read match status", "Some error", "error"])
        })

        it("should render error message if loading users data failed", () => {
            const props = {
                ...defaultProps,
                session: { status: OperationStatus.FAILED, error: new Error("Some error") },
            }
            const element = render(<MatchContainer {...props} />)
            const component = element.getByTestId("ALERT_MOCK")
            const textContents = Array.from(component.children).map((c) => c.textContent)
            expect(textContents).toEqual(["Unable to read users data", "Some error", "error"])
        })

        it("should render error message if loading members failed", () => {
            const props = {
                ...defaultProps,
                members: { status: OperationStatus.FAILED, error: new Error("Some error") },
            }
            const element = render(<MatchContainer {...props} />)
            const component = element.getByTestId("ALERT_MOCK")
            const textContents = Array.from(component.children).map((c) => c.textContent)
            expect(textContents).toEqual(["Unable to read game members", "Some error", "error"])
        })

        it("should render match view", () => {
            const props = { ...defaultProps }
            const element = render(<MatchContainer {...props} />)
            const component = element.getByTestId("MATCH_VIEW_MOCK")
            const textContents = Array.from(component.children).map((c) => c.textContent)
            expect(textContents).toEqual([
                JSON.stringify(game),
                JSON.stringify(round),
                JSON.stringify(session),
                JSON.stringify(members),
                JSON.stringify(scores),
            ])
        })
    })

    describe("Effect actions", () => {
        it("should fetch match status if current fetched has another id", () => {
            const fetchMock = jest.fn()
            const props = {
                ...defaultProps,
                fetchMatchState: fetchMock,
                matchState: { ...matchData, params: "another-game" },
            }
            render(<MatchContainer {...props} />)
            expect(fetchMock).toHaveBeenCalledTimes(1)
            expect(fetchMock).toHaveBeenCalledWith(game.id)
        })

        it("should fetch match status if not already fetched", () => {
            const fetchMock = jest.fn()
            const props = {
                ...defaultProps,
                fetchMatchState: fetchMock,
                matchState: { status: OperationStatus.NOT_STARTED },
            }
            render(<MatchContainer {...props} />)
            expect(fetchMock).toHaveBeenCalledTimes(1)
            expect(fetchMock).toHaveBeenCalledWith(game.id)
        })

        it("should fetch match status if previous failed", () => {
            const fetchMock = jest.fn()
            const props = {
                ...defaultProps,
                fetchMatchState: fetchMock,
                matchState: { status: OperationStatus.FAILED },
            }
            render(<MatchContainer {...props} />)
            expect(fetchMock).toHaveBeenCalledTimes(1)
            expect(fetchMock).toHaveBeenCalledWith(game.id)
        })

        it("should reset data on unmount", () => {
            const resetMock = jest.fn()
            const props = {
                ...defaultProps,
                resetMatchState: resetMock,
            }
            const element = render(<MatchContainer {...props} />)
            element.unmount()
            expect(resetMock).toHaveBeenCalledTimes(1)
        })

        it("should fetch current user data if missing", () => {
            const fetchUserMock = jest.fn()
            const props = {
                ...defaultProps,
                session: { status: OperationStatus.NOT_STARTED },
                fetchCurrentUser: fetchUserMock,
            }
            render(<MatchContainer {...props} />)
            expect(fetchUserMock).toHaveBeenCalledTimes(1)
        })

        it("should not fetch current user if request already in progress", () => {
            const fetchUserMock = jest.fn()
            const props = {
                ...defaultProps,
                session: { status: OperationStatus.PENDING },
                fetchCurrentUser: fetchUserMock,
            }
            render(<MatchContainer {...props} />)
            expect(fetchUserMock).not.toHaveBeenCalled()
        })

        it("should should fetch members data if missing", () => {
            const fetchMembersMock = jest.fn()
            const props = {
                ...defaultProps,
                members: { status: OperationStatus.NOT_STARTED },
                fetchGameMembers: fetchMembersMock,
            }
            render(<MatchContainer {...props} />)
            expect(fetchMembersMock).toHaveBeenCalledTimes(1)
            expect(fetchMembersMock).toHaveBeenCalledWith(game.id)
        })

        it("should should fetch members data request with different id is in progress", () => {
            const fetchMembersMock = jest.fn()
            const props = {
                ...defaultProps,
                members: { status: OperationStatus.PENDING, params: "other-game" },
                fetchGameMembers: fetchMembersMock,
            }
            render(<MatchContainer {...props} />)
            expect(fetchMembersMock).toHaveBeenCalledTimes(1)
            expect(fetchMembersMock).toHaveBeenCalledWith(game.id)
        })

        it("should connect websocket on mount", () => {
            const startWsMock = jest.fn()
            const props = {
                ...defaultProps,
                connectMatchWebSocket: startWsMock,
            }
            render(<MatchContainer {...props} />)
            expect(startWsMock).toHaveBeenCalledTimes(1)
            expect(startWsMock).toHaveBeenCalledWith(game.id)
        })

        it("should disconnect websocket on unmount", () => {
            const stopWsMock = jest.fn()
            const props = {
                ...defaultProps,
                disconnectMatchWebSocket: stopWsMock,
            }
            const element = render(<MatchContainer {...props} />)
            element.unmount()
            expect(stopWsMock).toHaveBeenCalledTimes(1)
            expect(stopWsMock).toHaveBeenCalledWith(game.id)
        })

        it("should show toast when player takes the trick", () => {
            const toast = jest.fn()
            const useToastMock = jest.spyOn(chakra, "useToast").mockImplementation(() => toastMock(toast))
            const props = {
                ...defaultProps,
                playerTakesTrick: { gameId: "foo-bar", player: "333" },
            }
            render(<MatchContainer {...props} />)
            expect(toast).toHaveBeenCalledTimes(1)
            expect(toast).toHaveBeenCalledWith({
                duration: 7000,
                isClosable: true,
                position: "top",
                title: "baz takes a trick",
            })
            useToastMock.mockRestore()
        })

        it("should show toast when nobody takes the trick", () => {
            const toast = jest.fn()
            const useToastMock = jest.spyOn(chakra, "useToast").mockImplementation(() => toastMock(toast))
            const props = {
                ...defaultProps,
                playerTakesTrick: { gameId: "foo-bar", player: null },
            }
            render(<MatchContainer {...props} />)
            expect(toast).toHaveBeenCalledTimes(1)
            expect(toast).toHaveBeenCalledWith({
                duration: 7000,
                isClosable: true,
                position: "top",
                title: "Nobody managed to take a trick",
            })
            useToastMock.mockRestore()
        })
    })
})
