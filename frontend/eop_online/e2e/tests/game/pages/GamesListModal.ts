import { ElementHandle } from "puppeteer"
import {
    COMPACT_TABLE_FIELD_CONTENT,
    COMPACT_TABLE_FIELD_ENTRY,
    COMPACT_TABLE_FIELD_HEADER,
    COMPACT_TABLE_RECORD,
} from "../../../../src/application/components/common/responsive_table/testids"
import { EMPTY_GAMES_LIST_PLACEHOLDER } from "../../../../src/domain/game/components/testids"
import {
    getAllByTestIdFrom,
    getAttributesOf,
    getByTextAndTagFrom,
    getTextContentByTestId,
} from "../../utils/page_utils"
export default class GamesListModal {
    private readonly modal: ElementHandle<Element>

    constructor(modal: ElementHandle<Element>) {
        this.modal = modal
    }

    async getAvailableGames() {
        const emptyGamesPlaceholder = await this.modal.$(`[data-testid='${EMPTY_GAMES_LIST_PLACEHOLDER}']`)
        if (emptyGamesPlaceholder) {
            return await getTextContentByTestId(this.modal, EMPTY_GAMES_LIST_PLACEHOLDER)
        } else {
            return await this.getAvailableGamesFromCompactTable(this.modal)
        }
    }

    private async getAvailableGamesFromCompactTable(modal: ElementHandle<Element>) {
        const records = await getAllByTestIdFrom(modal, COMPACT_TABLE_RECORD)
        return Promise.all(records.map(this.processCopaactTableRecord))
    }

    private async processCopaactTableRecord(record: ElementHandle<Element>) {
        const entries = await getAllByTestIdFrom(record, COMPACT_TABLE_FIELD_ENTRY)
        const pairs = await Promise.all(
            entries.map(async (entry) => {
                const header = await getTextContentByTestId(entry, COMPACT_TABLE_FIELD_HEADER)
                const content = await getTextContentByTestId(entry, COMPACT_TABLE_FIELD_CONTENT)
                return [header, content]
            })
        )
        return Object.fromEntries(pairs)
    }

    async navigateToGame(gameIndex: number) {
        const navigateButtons = await this.modal.$$("button[aria-label='Go to game']")
        if (gameIndex > navigateButtons.length - 1 || gameIndex < 0) {
            return Promise.reject(
                new Error(
                    `Unable to navigate to game with index ${gameIndex} because only ${navigateButtons.length} games found`
                )
            )
        }
        await navigateButtons[gameIndex].click()
    }

    async deleteGame(gameIndex: number, confirm?: boolean) {
        const deleteButton = await this.clickableDeleteButton(gameIndex)
        if (!deleteButton) {
            return Promise.reject(new Error("Delete button is disabled"))
        }
        await deleteButton.click()
        if (confirm === undefined) {
            return Promise.resolve()
        }
        const buttonText = confirm === true ? "Confirm" : "Cancel"
        const button = await getByTextAndTagFrom(this.modal, "button", buttonText)
        await button.click()
    }

    private async clickableDeleteButton(gameIndex: number) {
        const deleteButtons = await this.modal.$$("button[aria-label='Delete game']")
        if (gameIndex > deleteButtons.length - 1 || gameIndex < 0) {
            return Promise.reject(
                new Error(
                    `Unable to delete game with index ${gameIndex} because only ${deleteButtons.length} games found`
                )
            )
        }
        const buttonAttributes = await getAttributesOf(deleteButtons[gameIndex])
        if (buttonAttributes.hasOwnProperty("disabled")) {
            return null
        } else {
            return deleteButtons[gameIndex]
        }
    }

    async canDeleteGame(gameIndex: number) {
        const button = await this.clickableDeleteButton(gameIndex)
        return button !== null
    }
}
