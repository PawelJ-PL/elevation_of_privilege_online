import { render } from "@testing-library/react"
import React from "react"
import { OperationStatus } from "../../../../application/store/async/AsyncOperationResult"
import { members } from "../../../../testutils/constants/game"
import { Session } from "../../../user/types/Session"
import { Game } from "../../types/Game"
import { Member } from "../../types/Member"
import { AnteroomContainer } from "./AnteroomContainer"

/* eslint-disable react/display-name */

jest.mock("../../../../application/components/common/PageLoader")

jest.mock("../../../../application/components/common/AlertBox")

jest.mock("./AnteroomView", () => (props: { game: Game; currentUser: Session; members: Member[] }) => (
    <div data-testid="ANTEROOM_MOCK">
        <div>{JSON.stringify(props.game)}</div>
        <div>{JSON.stringify(props.currentUser)}</div>
        <div>{JSON.stringify(props.members)}</div>
    </div>
))

const game = { id: "foo-bar", description: null, creator: "baz", startedAt: null, finishedAt: null }
const session = { userId: "111", createdAt: "2020-11-23T23:12:00Z" }

describe("Anteroom container", () => {
    describe("View selection", () => {
        it("should render loader if user data fetching not started yet", () => {
            const userData = { status: OperationStatus.NOT_STARTED }
            const membersData = { status: OperationStatus.FINISHED, data: members }
            const element = render(
                <AnteroomContainer
                    game={game}
                    currentUser={userData}
                    members={membersData}
                    fetchGameMembers={jest.fn()}
                    fetchUserData={jest.fn()}
                />
            )
            const component = element.getByTestId("LOADER_MOCK")
            expect(component.textContent).toEqual("Loading user data")
        })

        it("should render loader if user data fetching is in progress", () => {
            const userData = { status: OperationStatus.PENDING }
            const membersData = { status: OperationStatus.FINISHED, data: members }
            const element = render(
                <AnteroomContainer
                    game={game}
                    currentUser={userData}
                    members={membersData}
                    fetchGameMembers={jest.fn()}
                    fetchUserData={jest.fn()}
                />
            )
            const component = element.getByTestId("LOADER_MOCK")
            expect(component.textContent).toEqual("Loading user data")
        })

        it("should render loader if members data fetching not started yet", () => {
            const userData = { status: OperationStatus.FINISHED, data: session }
            const membersData = { status: OperationStatus.NOT_STARTED }
            const element = render(
                <AnteroomContainer
                    game={game}
                    currentUser={userData}
                    members={membersData}
                    fetchGameMembers={jest.fn()}
                    fetchUserData={jest.fn()}
                />
            )
            const component = element.getByTestId("LOADER_MOCK")
            expect(component.textContent).toEqual("Loading game members")
        })

        it("should render loader if members data fetching is in progress", () => {
            const userData = { status: OperationStatus.FINISHED, data: session }
            const membersData = { status: OperationStatus.PENDING }
            const element = render(
                <AnteroomContainer
                    game={game}
                    currentUser={userData}
                    members={membersData}
                    fetchGameMembers={jest.fn()}
                    fetchUserData={jest.fn()}
                />
            )
            const component = element.getByTestId("LOADER_MOCK")
            expect(component.textContent).toEqual("Loading game members")
        })

        it("should render error message when loading user data has failed", () => {
            const userData = { status: OperationStatus.FAILED, error: new Error("Some error") }
            const membersData = { status: OperationStatus.FINISHED, data: members }
            const element = render(
                <AnteroomContainer
                    game={game}
                    currentUser={userData}
                    members={membersData}
                    fetchGameMembers={jest.fn()}
                    fetchUserData={jest.fn()}
                />
            )
            const component = element.getByTestId("ALERT_MOCK")
            const textContents = Array.from(component.children).map((c) => c.textContent)
            expect(textContents).toEqual(["Unable to load users data", "Some error", "error"])
        })

        it("should render error message when loading members data has failed", () => {
            const userData = { status: OperationStatus.FINISHED, data: session }
            const membersData = { status: OperationStatus.FAILED, error: new Error("Some error") }
            const element = render(
                <AnteroomContainer
                    game={game}
                    currentUser={userData}
                    members={membersData}
                    fetchGameMembers={jest.fn()}
                    fetchUserData={jest.fn()}
                />
            )
            const component = element.getByTestId("ALERT_MOCK")
            const textContents = Array.from(component.children).map((c) => c.textContent)
            expect(textContents).toEqual(["Unable to load game members", "Some error", "error"])
        })

        it("should render anteroom view", () => {
            const userData = { status: OperationStatus.FINISHED, data: session }
            const membersData = { status: OperationStatus.FINISHED, data: members }
            const element = render(
                <AnteroomContainer
                    game={game}
                    currentUser={userData}
                    members={membersData}
                    fetchGameMembers={jest.fn()}
                    fetchUserData={jest.fn()}
                />
            )
            const component = element.getByTestId("ANTEROOM_MOCK")
            const textContents = Array.from(component.children).map((c) => c.textContent)
            expect(textContents).toEqual([JSON.stringify(game), JSON.stringify(session), JSON.stringify(members)])
        })
    })
})
