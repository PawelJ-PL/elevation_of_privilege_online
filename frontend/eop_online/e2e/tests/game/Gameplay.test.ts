import { startServer, stopServer } from "./../utils/ws_utils"
import { meData, player2Id } from "./../constants"
import { Page } from "puppeteer"
import { exampleGame } from "../constants"
import { liftedScreenshotOptions } from "../utils/page_utils"
import { setupPuppeteer } from "../utils/puppeteer_setup"
import { matchers, RequestHandler, requestPredicates, setRequestHandlers } from "../utils/request_helper"
import GameplayPage from "./pages/GameplayPage"
import MainPage from "./pages/MainPage"
import { Server as WsServer } from "ws"

jest.setTimeout(10000)

let page: Page

let wsServer: WsServer

describe("Gameplay", () => {
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

    it("should join already started game", async () => {
        const handlers = [
            matchers.getDefaultGame,
            matchers.getMeData,
            matchers.getGameMembers,
            matchers.getDefaultMatch,
        ]
        await setRequestHandlers(page, { handlers })

        const mainPage = new MainPage(page)
        await mainPage.open()
        await mainPage.clickJoinGame()
        await mainPage.fillGameId(exampleGame.id)
        const gamePage = await mainPage.clickGoToGame()

        await gamePage.waitForLoaded()

        const leadingSuit = await gamePage.getLeadingSuit()
        expect(leadingSuit).toEqual("Tampering")

        const cardsOnTable = await gamePage.getCardsOnTable()
        expect(cardsOnTable).toStrictEqual([
            { player: "Alice", cardLink: "/cards/tampering-seven.png", threatLinked: true },
            { player: "Adam", cardLink: "/cards/denial-of-service-two.png", threatLinked: false },
        ])

        const players = await gamePage.getPlayers()
        expect(players).toStrictEqual([
            { name: "Pawel", scores: "5" },
            { name: "Alice", scores: "4" },
            { name: "Adam", scores: "3" },
        ])

        const hand = await gamePage.getHand()
        expect(hand).toEqual({
            suites: [
                { suit: "Denial of service", selected: true },
                { suit: "Elevation of privilege", selected: false },
                { suit: "Information disclosure", selected: false },
                { suit: "Repudiation", selected: false },
                { suit: "Spoofing", selected: false },
                { suit: "Tampering", selected: false },
            ],
            panels: [
                { hidden: false, cards: ["/cards/denial-of-service-jack.png"] },
                { hidden: true, cards: ["/cards/elevation-of-privilege-ace.png"] },
                { hidden: true, cards: ["/cards/information-disclosure-jack.png"] },
                { hidden: true, cards: ["/cards/repudiation-king.png"] },
                { hidden: true, cards: ["/cards/spoofing-four.png"] },
                { hidden: true, cards: ["/cards/tampering-nine.png"] },
            ],
        })
    })

    it("should be able to play card from current suit", async () => {
        const handlers = [
            matchers.getDefaultGame,
            matchers.getMeData,
            matchers.getGameMembers,
            matchers.getDefaultMatch,
        ]
        await setRequestHandlers(page, { handlers })

        const gamePage = new GameplayPage(page)
        await gamePage.open(exampleGame.id)
        await gamePage.selectSuit("Tampering")
        const currentHand = await gamePage.getHand()
        expect(currentHand.suites.find((s) => s.selected === true)).toStrictEqual({ suit: "Tampering", selected: true })
        expect(currentHand.panels.find((p) => p.hidden === false)).toStrictEqual({
            hidden: false,
            cards: ["/cards/tampering-nine.png"],
        })
        await gamePage.selectCardOnActivePanel(0)
        const canPlay = await gamePage.canPlayZoomedCard()
        expect(canPlay).toBe(true)
    })

    it("should not be able to play card from different suit", async () => {
        const handlers = [
            matchers.getDefaultGame,
            matchers.getMeData,
            matchers.getGameMembers,
            matchers.getDefaultMatch,
        ]
        await setRequestHandlers(page, { handlers })

        const gamePage = new GameplayPage(page)
        await gamePage.open(exampleGame.id)
        await gamePage.selectSuit("Spoofing")
        const currentHand = await gamePage.getHand()
        expect(currentHand.suites.find((s) => s.selected === true)).toStrictEqual({ suit: "Spoofing", selected: true })
        expect(currentHand.panels.find((p) => p.hidden === false)).toStrictEqual({
            hidden: false,
            cards: ["/cards/spoofing-four.png"],
        })
        await gamePage.selectCardOnActivePanel(0)
        const canPlay = await gamePage.canPlayZoomedCard()
        expect(canPlay).toBe(false)
    })

    it("should not be able to play card during other players turn", async () => {
        const getMeData: RequestHandler = {
            reqPredicate: requestPredicates.getCurrentUser,
            onMatch: (r) => r.respond({ body: JSON.stringify({ ...meData, userId: player2Id }) }),
        }
        const handlers = [matchers.getDefaultGame, getMeData, matchers.getGameMembers, matchers.getDefaultMatch]
        await setRequestHandlers(page, { handlers })

        const gamePage = new GameplayPage(page)
        await gamePage.open(exampleGame.id)
        await gamePage.selectSuit("Tampering")
        const currentHand = await gamePage.getHand()
        expect(currentHand.suites.find((s) => s.selected === true)).toStrictEqual({ suit: "Tampering", selected: true })
        expect(currentHand.panels.find((p) => p.hidden === false)).toStrictEqual({
            hidden: false,
            cards: ["/cards/tampering-nine.png"],
        })
        await gamePage.selectCardOnActivePanel(0)
        const canPlay = await gamePage.canPlayZoomedCard()
        expect(canPlay).toBe(false)
    })
})
