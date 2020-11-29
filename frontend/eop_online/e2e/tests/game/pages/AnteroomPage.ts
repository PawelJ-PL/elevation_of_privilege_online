import { getTextOfAllChildren, waitForElementWithText, waitForTestId } from "./../../utils/page_utils"
import { Page } from "puppeteer"

class AnteroomPage {
    private readonly page: Page

    private readonly WAITING_FOR_APPROVAL_TESTID = "elements-list-waiting-for-approval"
    private readonly ACCEPTED_PLAYERS_TESTID = "elements-list-accepted-players"
    private readonly OBSERVERS_TESTID = "elements-list-accepted-observers"

    constructor(page: Page) {
        this.page = page
    }

    async open(gameId: string) {
        await this.page.goto(`http://127.0.0.1:3000/game/${gameId}`, { waitUntil: "networkidle0" })
        this.waitForLoaded()
    }

    async waitForLoaded(timeout?: number) {
        await waitForTestId(
            this.page,
            "ANTEROOM_CONTAINER",
            { timeout: timeout ?? 3000 },
            { path: "waitForAnteroomContainer.png" }
        )
    }

    private async getMembersByTestId(testId: string) {
        const container = await waitForTestId(this.page, testId, { timeout: 300 }, { path: `get-${testId}.png` })
        const texts = await getTextOfAllChildren(container)
        return texts.slice(1)
    }

    getMembersWaitingForApproval() {
        return this.getMembersByTestId(this.WAITING_FOR_APPROVAL_TESTID)
    }

    getAcceptedPlayers() {
        return this.getMembersByTestId(this.ACCEPTED_PLAYERS_TESTID)
    }

    getAcceptedObservers() {
        return this.getMembersByTestId(this.OBSERVERS_TESTID)
    }

    async performActionOnMember(section: Sections, playerNumber: number, action: Actions) {
        const containerTestId = this.testIdBySection(section)
        const actionLabel = this.labelByAction(action)
        const container = await waitForTestId(this.page, containerTestId, { timeout: 300 })
        await container.evaluate(
            (c, label, position) => {
                const expectedChild = Array.from(c.children).slice(1)[position]
                if (expectedChild === undefined) {
                    return Promise.reject(new Error(`Player with position ${position} not found`))
                } else {
                    const buttons = Array.from(expectedChild.getElementsByTagName("button"))
                    const buttonWithLabel = buttons.find((b) => b.getAttribute("aria-label") === label)
                    if (!buttonWithLabel) {
                        return Promise.reject(`Action with label ${label} not allowed on user on position ${position}`)
                    } else {
                        buttonWithLabel.click()
                    }
                }
            },
            actionLabel,
            playerNumber
        )
    }

    private testIdBySection = (section: Sections) => {
        switch (section) {
            case "Waiting":
                return this.WAITING_FOR_APPROVAL_TESTID
            case "Players":
                return this.ACCEPTED_PLAYERS_TESTID
            case "Observers":
                return this.OBSERVERS_TESTID
        }
    }

    private labelByAction = (action: Actions) => {
        switch (action) {
            case "Kick":
                return "Kick"
            case "MakePlayer":
                return "Accept as a player"
            case "MakeObserver":
                return "Accept as an observer"
        }
    }

    async canStart() {
        const button = await waitForElementWithText(this.page, "button", "Start game", { timeout: 300 })
        const disabled = await button.evaluate((b) => b.hasAttribute("disabled"))
        return !disabled
    }
}

export default AnteroomPage

type Sections = "Waiting" | "Players" | "Observers"

type Actions = "MakeObserver" | "MakePlayer" | "Kick"
