import { getTextOfAllChildren, waitForTestId } from "./../../utils/page_utils"
import { Page } from "puppeteer"

class AnteroomPage {
    private readonly page: Page

    constructor(page: Page) {
        this.page = page
    }

    async waitForLoaded(timeout?: number) {
        await waitForTestId(
            this.page,
            "anteroom-container",
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
        return this.getMembersByTestId("elements-list-waiting-for-approval")
    }

    getAcceptedPlayers() {
        return this.getMembersByTestId("elements-list-accepted-players")
    }

    getAcceptedObservers() {
        return this.getMembersByTestId("elements-list-accepted-observers")
    }
}

export default AnteroomPage
