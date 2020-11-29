import { waitForElementWithText, waitForSelector, waitForTestId } from "./../../utils/page_utils"
import { Page } from "puppeteer"
import AnteroomPage from "./AnteroomPage"
import GameplayPage from "./GameplayPage"

class MainPage {
    private readonly page: Page

    constructor(page: Page) {
        this.page = page
    }

    async open() {
        await this.page.goto("http://127.0.0.1:3000")
        await waitForTestId(
            this.page,
            "MAIN_PAGE_BUTTONS_CONTAINER",
            { timeout: 2000 },
            { path: "loadingMainPage.png" }
        )
    }

    async clickCreateGame() {
        const button = await waitForElementWithText(
            this.page,
            "button",
            "Create new game",
            { timeout: 100 },
            { path: "clickCreateGame.png" }
        )
        button.click()
    }

    async clickJoinGame() {
        const button = await waitForElementWithText(
            this.page,
            "button",
            "Join game",
            { timeout: 100 },
            { path: "clickJoinGame.png" }
        )
        button.click()
    }

    async fillNickname(nickname: string) {
        await waitForSelector(this.page, "#nickname", { timeout: 300 }, { path: "typeNickname.png" })
        await this.page.type("#nickname", nickname)
    }

    async fillDescription(description: string) {
        await waitForSelector(this.page, "#description", { timeout: 300 }, { path: "typeDescription.png" })
        await this.page.type("#description", description)
    }

    async fillGameId(gameId: string) {
        await waitForSelector(this.page, "#gameIdField", { timeout: 300 }, { path: "typeGameId.png" })
        await this.page.type("#gameIdField", gameId)
    }

    async clickCreateConfirmation() {
        const confirmButton = await waitForElementWithText(
            this.page,
            "button",
            "Create",
            { timeout: 300 },
            { path: "clickGameCreateConfirmation.png" }
        )
        await Promise.all([confirmButton.click(), this.page.waitForNavigation])
        return new AnteroomPage(this.page)
    }

    async clickGoToGame() {
        const confirmButton = await waitForElementWithText(
            this.page,
            "button",
            "Continue",
            { timeout: 300 },
            { path: "clickGoToGame.png" }
        )
        await Promise.all([confirmButton.click(), this.page.waitForNavigation])
        return new GameplayPage(this.page)
    }
}

export default MainPage
