import {
    getAllByRoleFrom,
    getAllByTestId,
    getAllByTextAndTagFrom,
    getByRoleFrom,
} from "./../../utils/page_utils"
import { ElementHandle, Page } from "puppeteer"
import { waitForTestId } from "../../utils/page_utils"

class GameplayPage {
    private readonly page: Page

    constructor(page: Page) {
        this.page = page
    }

    async open(gameId: string) {
        await this.page.goto(`http://127.0.0.1:3000/game/${gameId}`)
        await this.waitForLoaded()
    }

    private imagesHaveLoaded() {
        const images = Array.from(document.images)
        return images.every((i) => i.complete) && !images.some((i) => i.classList.contains("chakra-image__placeholder"))
    }

    async waitForLoaded(timeout?: number) {
        const waitOptions = { timeout: timeout ?? 3000 }

        await waitForTestId(this.page, "LEADING_SUIT_BOX", waitOptions, { path: "waitForLeadingSuitBox.png" })

        await waitForTestId(this.page, "TABLE_CARDS", waitOptions, { path: "waitForTableBox.png" })

        await waitForTestId(this.page, "PLAYERS_LIST_ENTRY", waitOptions, { path: "waitForPlayerEntry.png" })

        await waitForTestId(this.page, "PLAYERS_HAND", waitOptions, { path: "waitForPlayersHand.png" })

        await this.page.waitForFunction(this.imagesHaveLoaded, waitOptions)
    }

    async getLeadingSuit() {
        const box = await waitForTestId(
            this.page,
            "LEADING_SUIT_BOX",
            { timeout: 300 },
            { path: "getLeadingSuitBox.png" }
        )
        return await box.evaluate((b) => b.lastElementChild?.lastElementChild?.lastElementChild?.textContent)
    }

    async getCardsOnTable(): Promise<
        Array<{ player?: string | null; cardLink?: string | null; threatLinked: boolean | null }>
    > {
        const allCardsContainer = await waitForTestId(this.page, "TABLE_CARDS")
        const cards = await allCardsContainer.evaluate((container) =>
            Array.from(container?.children).map((element) => {
                const player = element.firstChild?.textContent
                const cardLink = element.lastElementChild?.getAttribute("src")
                const threatLinkedLabel = element.firstElementChild?.nextElementSibling?.getAttribute("aria-label")
                let threatLinked: boolean | null
                if (threatLinkedLabel === "Threat linked") {
                    threatLinked = true
                } else if (threatLinkedLabel === "Threat not linked") {
                    threatLinked = false
                } else {
                    threatLinked = null
                }
                return { player, cardLink, threatLinked }
            })
        )
        return cards
    }

    async getPlayers() {
        const entries = await getAllByTestId(this.page, "PLAYERS_LIST_ENTRY", undefined, {
            path: "getAllPlayerEntries.png",
        })
        return await Promise.all(entries.map(this.processPlayerNode))
    }

    private processPlayerNode = (element: ElementHandle<Element>) =>
        element.evaluate((e) => {
            const name = e.firstElementChild?.textContent
            const scores = e.lastElementChild?.textContent
            return { name, scores }
        })

    async getHand() {
        const handConatainer = await waitForTestId(
            this.page,
            "PLAYERS_HAND",
            { timeout: 300 },
            { path: "waitForPlayersHand.png" }
        )
        const tabListElement = await getByRoleFrom(handConatainer, "tablist")
        const tabs = await getAllByRoleFrom(tabListElement, "tab")
        const suites = await Promise.all(tabs.map(this.processSuitTabNode))
        const panelElements = await getAllByRoleFrom(handConatainer, "tabpanel")
        const panels = await Promise.all(panelElements.map(this.processPanelNode))
        return { suites, panels }
    }

    private processSuitTabNode = (element: ElementHandle<Element>) => {
        return element.evaluate((t) => ({
            suit: t.textContent,
            selected: t.getAttribute("aria-selected") === "true",
        }))
    }

    private processPanelNode = (element: ElementHandle<Element>) =>
        element.evaluate((p) => ({
            hidden: p.hasAttribute("hidden"),
            cards: Array.from(p.getElementsByTagName("img")).map((img) => img.getAttribute("src")),
        }))

    async selectSuit(suit: Suites) {
        const hand = await this.getHand()
        const suitIndex = hand.suites.map((s) => s.suit).indexOf(suit)
        if (suitIndex === -1) {
            return Promise.reject(
                `No suit ${suit} on Hand. Available suites: ${hand.suites.map((s) => s.suit).join(", ")}`
            )
        }
        const handConatainer = await waitForTestId(
            this.page,
            "PLAYERS_HAND",
            { timeout: 300 },
            { path: "waitForPlayersHand.png" }
        )
        const tabListElement = await getByRoleFrom(handConatainer, "tablist")
        const tabs = await getAllByRoleFrom(tabListElement, "tab")
        await tabs[suitIndex].click()
    }

    async selectCardOnActivePanel(cardIndex: number) {
        const handConatainer = await waitForTestId(
            this.page,
            "PLAYERS_HAND",
            { timeout: 300 },
            { path: "waitForPlayersHand.png" }
        )
        const hand = await this.getHand()
        const panelIndex = hand.panels.map((p) => p.hidden).indexOf(false)
        const activePanel = await getAllByRoleFrom(handConatainer, "tabpanel").then((p) => p[panelIndex])
        await activePanel.evaluate((p, idx) => {
            const images = Array.from(p.getElementsByTagName("img"))
            if (images[idx] === undefined) {
                return Promise.reject(new Error("No card with index " + idx))
            } else {
                images[idx].click()
            }
        }, cardIndex)
    }

    async canPlayZoomedCard() {
        const footer = await waitForTestId(
            this.page,
            "ZOOM_CARD_MODAL_FOOTER",
            { timeout: 1000 },
            { path: "waitForZoomedCardFooter.png" }
        )
        const playButtons = await getAllByTextAndTagFrom(footer, "button", "Play this card")
        return playButtons.length > 0
    }
}

export default GameplayPage

export type Suites =
    | "Denial of service"
    | "Elevation of privilege"
    | "Information disclosure"
    | "Repudiation"
    | "Spoofing"
    | "Tampering"
