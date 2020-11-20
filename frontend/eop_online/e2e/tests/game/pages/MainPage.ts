import { waitForElementWithText, waitForSelector } from "./../../utils/page_utils"
import { Page } from "puppeteer"
import AnteroomPage from "./AnteroomPage"

class MainPage {
    private readonly page: Page

    constructor(page: Page) {
        this.page = page
    }

    async open() {
        await this.page.goto("http://127.0.0.1:3000")

        await waitForElementWithText(
            this.page,
            "button",
            "Create new game",
            { timeout: 2000 },
            { path: "loadMainPage.png" }
        )
        await waitForElementWithText(this.page, "button", "Join game", { timeout: 1000 }, { path: "loadMainPage.png" })
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

    async fillNickname(nickname: string) {
        await waitForSelector(this.page, "#nickname", { timeout: 300 }, { path: "typeNickname.png" })
        await this.page.type("#nickname", nickname)
    }

    async fillDescription(description: string) {
        await waitForSelector(this.page, "#description", { timeout: 300 }, { path: "typeDescription.png" })
        await this.page.type("#description", description)
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
}

export default MainPage
