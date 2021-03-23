import { waitForSelector } from "./../utils/page_utils"
import { exampleUsersGame2 } from "./../constants"
import { matchers, RequestHandler, requestPredicates, setRequestHandlers } from "./../utils/request_helper"
import { Page } from "puppeteer"
import { Server as WsServer } from "ws"
import { liftedScreenshotOptions } from "../utils/page_utils"
import { setupPuppeteer } from "../utils/puppeteer_setup"
import { startServer, stopServer } from "../utils/ws_utils"
import MainPage from "./pages/MainPage"

// jest.setTimeout(10000)
jest.setTimeout(100000000) // FIXME

let page: Page

let wsServer: WsServer

describe("List games", () => {
    beforeEach(async () => {
        const env = await setupPuppeteer()
        page = env.page

        const ws = await startServer()
        wsServer = ws.server
    })

    afterEach(async () => {
        await page.screenshot(liftedScreenshotOptions({ path: "afterTest.png" }, "_finished"))
        await page.close()

        await stopServer(wsServer)
    })

    it("should list available games", async () => {
        const handlers = [matchers.listDefaultGames]
        await setRequestHandlers(page, { handlers })

        const mainPage = new MainPage(page)
        await mainPage.open()
        const gamesList = await mainPage.clickContinueGame()
        const games = await gamesList.getAvailableGames()
        expect(games).toStrictEqual([
            {
                Navigate: "",
                Description: "First game",
                "Game owner": "You",
                "Your nickname": "Pawel",
                Role: "Player",
                Started: "No",
                Finished: "No",
                "Delete game": "",
            },
            {
                Navigate: "",
                Description: "",
                "Game owner": "Alice",
                "Your nickname": "Pawel",
                Role: "Observer",
                Started: "No",
                Finished: "No",
                "Delete game": "",
            },
            {
                Navigate: "",
                Description: "",
                "Game owner": "You",
                "Your nickname": "Pawel",
                Role: "Player",
                Started: "No",
                Finished: "No",
                "Delete game": "",
            },
        ])
    })

    it("should display no games placeholder", async () => {
        const gamesListHandler: RequestHandler = {
            reqPredicate: requestPredicates.listAvailableGames,
            onMatch: (r) => r.respond({ body: JSON.stringify([]) }),
        }
        await setRequestHandlers(page, { handlers: [gamesListHandler] })

        const mainPage = new MainPage(page)
        await mainPage.open()
        const gamesList = await mainPage.clickContinueGame()
        const games = await gamesList.getAvailableGames()
        expect(games).toEqual("No previous games found")
    })

    it("should navigate to game", async () => {
        const handlers = [matchers.listDefaultGames]
        await setRequestHandlers(page, { handlers })

        const mainPage = new MainPage(page)
        await mainPage.open()
        const gamesList = await mainPage.clickContinueGame()
        await gamesList.navigateToGame(1)
        await waitForSelector(page, "div[role='alert']", { timeout: 1000 }, { path: "waitForErrorAlert.png" })
        expect(page.url().split("/").pop()).toEqual(exampleUsersGame2.id)
    })

    it("should delete game", async () => {
        const handlers = [matchers.listDefaultGames, matchers.deleteDefaultGame]
        await setRequestHandlers(page, { handlers })

        const mainPage = new MainPage(page)
        await mainPage.open()
        const gamesList = await mainPage.clickContinueGame()
        const gamesBefore = await gamesList.getAvailableGames()
        expect(gamesBefore).toStrictEqual([
            {
                Navigate: "",
                Description: "First game",
                "Game owner": "You",
                "Your nickname": "Pawel",
                Role: "Player",
                Started: "No",
                Finished: "No",
                "Delete game": "",
            },
            {
                Navigate: "",
                Description: "",
                "Game owner": "Alice",
                "Your nickname": "Pawel",
                Role: "Observer",
                Started: "No",
                Finished: "No",
                "Delete game": "",
            },
            {
                Navigate: "",
                Description: "",
                "Game owner": "You",
                "Your nickname": "Pawel",
                Role: "Player",
                Started: "No",
                Finished: "No",
                "Delete game": "",
            },
        ])

        await gamesList.deleteGame(0, true)
        await page.waitForResponse(r => r.status() === 201, {timeout: 1000})
        const gamesAfter = await gamesList.getAvailableGames()
        expect(gamesAfter).toStrictEqual([
            {
                Navigate: "",
                Description: "",
                "Game owner": "Alice",
                "Your nickname": "Pawel",
                Role: "Observer",
                Started: "No",
                Finished: "No",
                "Delete game": "",
            },
            {
                Navigate: "",
                Description: "",
                "Game owner": "You",
                "Your nickname": "Pawel",
                Role: "Player",
                Started: "No",
                Finished: "No",
                "Delete game": "",
            },
        ])
    })

    it("should not be able to delete other users game", async () => {
        const handlers = [matchers.listDefaultGames]
        await setRequestHandlers(page, { handlers })

        const mainPage = new MainPage(page)
        await mainPage.open()
        const gamesList = await mainPage.clickContinueGame()
        const canDelete = await gamesList.canDeleteGame(1)
        expect(canDelete).toBe(false)
    })
})
