import { setupPuppeteer } from "./../utils/puppeteer_setup"
import { Page } from "puppeteer"
import MainPage from "./pages/MainPage"
import { matchers, setRequestHandlers } from "../utils/request_helper"
import { exampleGame, initMember } from "../constants"

jest.setTimeout(10000)

let page: Page

describe("Create game", () => {
    beforeEach(async () => {
        const env = await setupPuppeteer()
        page = env.page
    })

    afterEach(async () => {
        await page.close()
    })

    it("should open anteroom", async () => {
        const handlers = [
            matchers.createGameSuccess,
            matchers.getDefaultGame,
            matchers.getMeData,
            matchers.getInitMembers,
        ]
        await setRequestHandlers(page, { handlers })

        const mainPage = new MainPage(page)
        await mainPage.open()
        await mainPage.clickCreateGame()
        await mainPage.fillNickname(initMember.nickname)
        await mainPage.fillDescription(exampleGame.description)
        const anteroomPage = await mainPage.clickCreateConfirmation()

        await anteroomPage.waitForLoaded(3000)
        const waitingMembers = await anteroomPage.getMembersWaitingForApproval()
        const players = await anteroomPage.getAcceptedPlayers()
        const observers = await anteroomPage.getAcceptedObservers()

        expect(waitingMembers).toHaveLength(0)
        expect(players).toEqual([initMember.nickname])
        expect(observers).toHaveLength(0)
    })
})

export {}
