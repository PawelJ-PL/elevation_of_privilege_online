import { Page } from "puppeteer"
import { liftedScreenshotOptions } from "../utils/page_utils"
import { setupPuppeteer } from "../utils/puppeteer_setup"
import { matchers, RequestHandler, requestPredicates, setRequestHandlers } from "../utils/request_helper"
import { exampleGame } from "../constants"
import FinishedGamePage from "./pages/FinishedGamePage"

jest.setTimeout(10000)

let page: Page

describe("Game finished", () => {
    beforeEach(async () => {
        const env = await setupPuppeteer()
        page = env.page
    })

    afterEach(async () => {
        await page.screenshot(liftedScreenshotOptions({ path: "afterTest.png" }, "_finished"))
        await page.close()
    })

    it("should show game summary", async () => {
        const getFinishedGame: RequestHandler = {
            reqPredicate: requestPredicates.getDefaultGameData,
            onMatch: (r) => r.respond({ body: JSON.stringify({ ...exampleGame, finishedAt: "2020-11-23T23:40:00Z" }) }),
        }
        const handlers = [getFinishedGame, matchers.getMeData, matchers.getMembers, matchers.getDefaultScores]
        await setRequestHandlers(page, { handlers })

        const summaryPage = new FinishedGamePage(page)
        await summaryPage.open(exampleGame.id)
        const ranking = await summaryPage.getRanking()
        expect(ranking).toEqual(["Pawel (5 points)", "Alice (4 points)", "Adam (3 points)"])
    })
})
