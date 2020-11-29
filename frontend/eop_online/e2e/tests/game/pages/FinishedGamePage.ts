import { getAllByTestIdFrom, waitForTestId } from "./../../utils/page_utils"
import { ElementHandle, Page } from "puppeteer"

class FinishedGamePage {
    private readonly page: Page

    constructor(page: Page) {
        this.page = page
    }

    async open(gameId: string) {
        await this.page.goto(`http://127.0.0.1:3000/game/${gameId}`, { waitUntil: "networkidle0" })
        // this.waitForLoaded()
    }

    async waitForLoaded() {
        await waitForTestId(this.page, "elements-list-players", { timeout: 1000 })
    }

    async getRanking() {
        const container = await waitForTestId(this.page, "elements-list-players", { timeout: 300 })
        const entries = await getAllByTestIdFrom(container, "PLAYER_RANKING_ENTRY")
        return await Promise.all(entries.map(this.getPlayerFromRankingEntry))
    }

    private getPlayerFromRankingEntry = (element: ElementHandle<Element>) =>
        element.evaluate((e) => Array.from(e.children)[1].textContent)
}

export default FinishedGamePage
