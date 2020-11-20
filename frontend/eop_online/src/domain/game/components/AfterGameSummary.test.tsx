import { render } from "@testing-library/react"
import React from "react"
import { OperationStatus } from "../../../application/store/async/AsyncOperationResult"
import { members, scores } from "../../../testutils/constants/game"
import { AfterGameSummary } from "./AfterGameSummary"
import { PLAYER_RANKING_ENTRY } from "./testids"

/* eslint-disable react/display-name */

jest.mock("../../../application/components/common/AlertBox")

jest.mock("@chakra-ui/core", () => ({
    ...jest.requireActual("@chakra-ui/core"),
    Skeleton: () => <div data-testid="SKELETON_MOCK"></div>,
    Icon: (props: { color: string }) => <div data-testid="ICON_MOCK" color={props.color}></div>,
}))

jest.mock("")

const gameData = { id: "foo-bar", description: null, creator: "baz", startedAt: null, finishedAt: null }

describe("After game summary", () => {
    describe("Render", () => {
        describe("Error alert", () => {
            it("should be rendered if fetching scores failed", () => {
                const fetchMembers = jest.fn()
                const fetchScores = jest.fn()
                const membersData = { status: OperationStatus.FINISHED, data: members }
                const scoresData = { status: OperationStatus.FAILED, error: new Error("Some error") }

                const element = render(
                    <AfterGameSummary
                        game={gameData}
                        fetchMembers={fetchMembers}
                        fetchScores={fetchScores}
                        members={membersData}
                        scores={scoresData}
                    />
                )
                const component = element.getByTestId("ALERT_MOCK")
                const textContents = Array.from(component.children).map((c) => c.textContent)
                expect(textContents).toEqual(["Unable to fetch winners data", "Some error", "error"])
            })

            it("should be rendered if fetching members failed", () => {
                const fetchMembers = jest.fn()
                const fetchScores = jest.fn()
                const membersData = { status: OperationStatus.FAILED, error: new Error("Some error") }
                const scoresData = { status: OperationStatus.FINISHED, data: scores }

                const element = render(
                    <AfterGameSummary
                        game={gameData}
                        fetchMembers={fetchMembers}
                        fetchScores={fetchScores}
                        members={membersData}
                        scores={scoresData}
                    />
                )
                const component = element.getByTestId("ALERT_MOCK")
                const textContents = Array.from(component.children).map((c) => c.textContent)
                expect(textContents).toEqual(["Unable to fetch winners data", "Some error", "error"])
            })
        })

        describe("Loading placeholder", () => {
            it("should be rendered if members data is missing", () => {
                const fetchMembers = jest.fn()
                const fetchScores = jest.fn()
                const membersData = { status: OperationStatus.PENDING }
                const scoresData = { status: OperationStatus.FINISHED, data: scores }

                const element = render(
                    <AfterGameSummary
                        game={gameData}
                        fetchMembers={fetchMembers}
                        fetchScores={fetchScores}
                        members={membersData}
                        scores={scoresData}
                    />
                )

                const component = element.queryByTestId("SKELETON_MOCK")
                expect(component).not.toBe(null)
            })

            it("should be rendered if scores data is missing", () => {
                const fetchMembers = jest.fn()
                const fetchScores = jest.fn()
                const membersData = { status: OperationStatus.FINISHED, data: members }
                const scoresData = { status: OperationStatus.PENDING }

                const element = render(
                    <AfterGameSummary
                        game={gameData}
                        fetchMembers={fetchMembers}
                        fetchScores={fetchScores}
                        members={membersData}
                        scores={scoresData}
                    />
                )

                const component = element.queryByTestId("SKELETON_MOCK")
                expect(component).not.toBe(null)
            })
        })

        describe("Player entries", () => {
            it("should be rendered in proper oreder", () => {
                const fetchMembers = jest.fn()
                const fetchScores = jest.fn()
                const membersData = { status: OperationStatus.FINISHED, data: members }
                const scoresData = { status: OperationStatus.FINISHED, data: scores }

                const element = render(
                    <AfterGameSummary
                        game={gameData}
                        fetchMembers={fetchMembers}
                        fetchScores={fetchScores}
                        members={membersData}
                        scores={scoresData}
                    />
                )
                const entries = element.getAllByTestId(PLAYER_RANKING_ENTRY)
                const textContents = entries.map((e) => e.textContent)
                expect(textContents).toEqual([
                    "1) baz (15 points)",
                    "2) quux (10 points)",
                    "3) foo (8 points)",
                    "4) quuz (5 points)",
                ])
            })

            it("should be rendered with cups", () => {
                const fetchMembers = jest.fn()
                const fetchScores = jest.fn()
                const membersData = { status: OperationStatus.FINISHED, data: members }
                const scoresData = { status: OperationStatus.FINISHED, data: scores }

                const element = render(
                    <AfterGameSummary
                        game={gameData}
                        fetchMembers={fetchMembers}
                        fetchScores={fetchScores}
                        members={membersData}
                        scores={scoresData}
                    />
                )
                const entries = element.getAllByTestId(PLAYER_RANKING_ENTRY)
                const cupsColors = entries
                    .map((element) =>
                        Array.from(element.children).find((c) => c.getAttribute("data-testid") === "ICON_MOCK")
                    )
                    .map((e) => e?.getAttribute("color"))
                expect(cupsColors).toEqual(["yellow.500", "gray.400", "yellow.800", undefined])
            })
        })
    })
})
