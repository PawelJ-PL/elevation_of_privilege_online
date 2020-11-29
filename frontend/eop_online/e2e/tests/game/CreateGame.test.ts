import { matcherCreators } from "./../utils/request_helper"
import { setupPuppeteer } from "./../utils/puppeteer_setup"
import { Page } from "puppeteer"
import MainPage from "./pages/MainPage"
import { matchers, RequestHandler, requestPredicates, setRequestHandlers } from "../utils/request_helper"
import { exampleGame, initMember, player2, player3 } from "../constants"
import { liftedScreenshotOptions } from "../utils/page_utils"
import { startServer, stopServer } from "../utils/ws_utils"
import AnteroomPage from "./pages/AnteroomPage"
import { Server as WsServer } from "ws"

jest.setTimeout(10000)

let page: Page

let wsServer: WsServer

describe("Create game", () => {
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

    it("should open anteroom", async () => {
        const getNotStartedGame: RequestHandler = {
            reqPredicate: requestPredicates.getDefaultGameData,
            onMatch: (r) => r.respond({ body: JSON.stringify({ ...exampleGame, startedAt: null }) }),
        }
        const handlers = [matchers.createGameSuccess, getNotStartedGame, matchers.getMeData, matchers.getInitMembers]
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

    it("should be able to start new game", async () => {
        const getNotStartedGame: RequestHandler = {
            reqPredicate: requestPredicates.getDefaultGameData,
            onMatch: (r) => r.respond({ body: JSON.stringify({ ...exampleGame, startedAt: null }) }),
        }
        const initPlayer2 = { ...player2, role: undefined }
        const initPlayer3 = { ...player3, role: undefined }
        const getInitMembers: RequestHandler = {
            reqPredicate: requestPredicates.getDefaultGameMembers,
            onMatch: (r) => r.respond({ body: JSON.stringify([initMember, initPlayer2, initPlayer3]) }),
        }
        const deleteMember3 = matcherCreators.deleteMember(player3.id, exampleGame.id, wsServer.clients)
        const setRoleObserverForPlayer2 = matcherCreators.setRole(
            player2.id,
            "Observer",
            exampleGame.id,
            wsServer.clients
        )
        const setRolePlayerForPlayer2 = matcherCreators.setRole(player2.id, "Player", exampleGame.id, wsServer.clients)
        const handlers = [
            getNotStartedGame,
            matchers.getMeData,
            getInitMembers,
            deleteMember3,
            setRoleObserverForPlayer2,
            setRolePlayerForPlayer2,
        ]
        await setRequestHandlers(page, { handlers })

        const anteroomPage = new AnteroomPage(page)
        await anteroomPage.open(exampleGame.id)

        const waitingMembersStep1 = await anteroomPage.getMembersWaitingForApproval()
        const playersStep1 = await anteroomPage.getAcceptedPlayers()
        const observersStep1 = await anteroomPage.getAcceptedObservers()
        expect(waitingMembersStep1).toStrictEqual(["Alice", "Adam"])
        expect(playersStep1).toStrictEqual(["Pawel"])
        expect(observersStep1).toHaveLength(0)

        const canStartStep1 = await anteroomPage.canStart()
        expect(canStartStep1).toBe(false)

        await anteroomPage.performActionOnMember("Waiting", 1, "Kick")

        const waitingMembersStep2 = await anteroomPage.getMembersWaitingForApproval()
        const playersUpdatedStep2 = await anteroomPage.getAcceptedPlayers()
        const observersUpdatedStep2 = await anteroomPage.getAcceptedObservers()
        expect(waitingMembersStep2).toStrictEqual(["Alice"])
        expect(playersUpdatedStep2).toStrictEqual(["Pawel"])
        expect(observersUpdatedStep2).toHaveLength(0)

        const canStartStep2 = await anteroomPage.canStart()
        expect(canStartStep2).toBe(false)

        await anteroomPage.performActionOnMember("Waiting", 0, "MakeObserver")
        const waitingMembersStep3 = await anteroomPage.getMembersWaitingForApproval()
        const playersUpdatedStep3 = await anteroomPage.getAcceptedPlayers()
        const observersUpdatedStep3 = await anteroomPage.getAcceptedObservers()
        expect(waitingMembersStep3).toHaveLength(0)
        expect(playersUpdatedStep3).toStrictEqual(["Pawel"])
        expect(observersUpdatedStep3).toStrictEqual(["Alice"])

        const canStartStep3 = await anteroomPage.canStart()
        expect(canStartStep3).toBe(false)

        await anteroomPage.performActionOnMember("Observers", 0, "MakePlayer")
        const waitingMembersStep4 = await anteroomPage.getMembersWaitingForApproval()
        const playersUpdatedStep4 = await anteroomPage.getAcceptedPlayers()
        const observersUpdatedStep4 = await anteroomPage.getAcceptedObservers()
        expect(waitingMembersStep4).toHaveLength(0)
        expect(playersUpdatedStep4).toStrictEqual(["Pawel", "Alice"])
        expect(observersUpdatedStep4).toHaveLength(0)

        const canStartStep4 = await anteroomPage.canStart()
        expect(canStartStep4).toBe(true)
    })
})

export {}
