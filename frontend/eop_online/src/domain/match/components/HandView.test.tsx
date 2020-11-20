import { render, fireEvent, RenderResult } from "@testing-library/react"
import React from "react"
import { cards, round } from "../../../testutils/constants/match"
import { Session } from "../../user/types/Session"
import { Suit } from "../types/Card"
import { Round } from "../types/Round"
import HandView from "./HandView"

/* eslint-disable react/display-name */

jest.mock("./CardZoomModal", () => (props: { canPlayCard: boolean; visible: boolean }) => (
    <div data-testid="ZOOM_MODAL_MOCK">
        <div>{JSON.stringify(props.canPlayCard)}</div>
        <div>{JSON.stringify(props.visible)}</div>
    </div>
))

jest.mock("@chakra-ui/core", () => ({
    ...jest.requireActual("@chakra-ui/core"),
    useBreakpointValue: () => true,
}))

const session = { userId: "111", createdAt: "2020-11-23T23:12:00Z" }

describe("Hand view", () => {
    const clickSuitTab = (element: RenderResult, suitDescription: string) => {
        const tabs = element.getAllByRole("tab")
        const tabIndex = tabs.map((t) => t.textContent).indexOf(suitDescription)
        expect(tabIndex).toBeGreaterThan(-1)
        const tab = tabs[tabIndex]
        fireEvent.click(tab)
        return tabIndex
    }

    const clickFirstImageInActiveTabPanel = (element: RenderResult, expectedTabIndex?: number) => {
        const tabpanel = element.getByRole("tabpanel")
        expectedTabIndex !== undefined
            ? expect(tabpanel.getAttribute("id")?.endsWith(`--tabpanel-${expectedTabIndex}`)).toBe(true)
            : void 0
        const images = Array.from(tabpanel.getElementsByTagName("img"))
        expect(images.length).toBeGreaterThan(0)
        const firstImage = images[0]
        fireEvent.click(firstImage)
    }

    const verifyZoomModal = (
        round: Round,
        currentUser: Session,
        playFromSuite: string,
        shouldBeVisible: boolean,
        shouldCardBePlayable: boolean
    ) => {
        const element = render(<HandView round={round} currentUser={currentUser} />)
        const tabIndex = clickSuitTab(element, playFromSuite)
        clickFirstImageInActiveTabPanel(element, tabIndex)
        const zoomModal = element.getByTestId("ZOOM_MODAL_MOCK")
        const textContents = Array.from(zoomModal.children).map((c) => c.textContent)
        expect(textContents).toEqual([String(shouldCardBePlayable), String(shouldBeVisible)])
    }

    it("should allow to play zoomed card from leading suit", () => {
        const leadingSuit: Suit = "InformationDisclosure"
        const hand = [
            cards.spoofing.four,
            cards.tampering.seven,
            cards.repudiation.king,
            cards.informationDisclosure.jack,
            cards.denialOfService.two,
            cards.elevationOfPrivilege.ace,
        ]
        const roundState = { ...round.state, currentPlayer: session.userId, leadingSuit }
        const testRound = { ...round, state: roundState, hand }
        verifyZoomModal(testRound, session, "Information disclosure", true, true)
    })

    it("should not allow to play zoomed card if another users turn", () => {
        const leadingSuit: Suit = "InformationDisclosure"
        const hand = [
            cards.spoofing.four,
            cards.tampering.seven,
            cards.repudiation.king,
            cards.informationDisclosure.jack,
            cards.denialOfService.two,
            cards.elevationOfPrivilege.ace,
        ]
        const roundState = { ...round.state, currentPlayer: session.userId, leadingSuit }
        const testRound = { ...round, state: roundState, hand }
        const testSession = { ...session, userId: "222" }
        verifyZoomModal(testRound, testSession, "Information disclosure", true, false)
    })

    it("should not allow to play zoomed card from different suit", () => {
        const leadingSuit: Suit = "InformationDisclosure"
        const hand = [
            cards.spoofing.four,
            cards.tampering.seven,
            cards.repudiation.king,
            cards.informationDisclosure.jack,
            cards.denialOfService.two,
            cards.elevationOfPrivilege.ace,
        ]
        const roundState = { ...round.state, currentPlayer: session.userId, leadingSuit }
        const testRound = { ...round, state: roundState, hand }
        verifyZoomModal(testRound, session, "Spoofing", true, false)
    })

    it("should allow to play zoomed card from different suit if user has no card in leading suit", () => {
        const leadingSuit: Suit = "InformationDisclosure"
        const hand = [
            cards.spoofing.four,
            cards.tampering.seven,
            cards.repudiation.king,
            cards.denialOfService.two,
            cards.elevationOfPrivilege.ace,
        ]
        const roundState = { ...round.state, currentPlayer: session.userId, leadingSuit }
        const testRound = { ...round, state: roundState, hand }
        verifyZoomModal(testRound, session, "Spoofing", true, true)
    })

    it("should allow to play zoomed card from any suit when leading suit is not defined", () => {
        const leadingSuit = null
        const hand = [
            cards.spoofing.four,
            cards.tampering.seven,
            cards.repudiation.king,
            cards.informationDisclosure.jack,
            cards.denialOfService.two,
            cards.elevationOfPrivilege.ace,
        ]
        const roundState = { ...round.state, currentPlayer: session.userId, leadingSuit }
        const testRound = { ...round, state: roundState, hand }
        verifyZoomModal(testRound, session, "Spoofing", true, true)
    })
})
