import { Browser, ConsoleMessage, ConsoleMessageType } from "puppeteer"
import chalk from "chalk"
import puppeteer from "puppeteer"

let browser: Browser

afterAll(async () => {
    if (browser) {
        await browser.close()
    }
})

beforeAll(async () => {
    if (!browser) {
        browser = await puppeteer.launch({
            headless: process.env.WITH_BROWSER !== "true",
            args: ["--no-sandbox", "--disable-setuid-sandbox"],
        })
    }
})

export const setupPuppeteer = async () => {
    const page = await browser.newPage()
    page.on("console", redirectConsoleMessage)
    page.on("error", redirectError)
    page.on("pageerror", redirectError)

    return { page }
}

const redirectConsoleMessage = (message: ConsoleMessage) => {
    const messageColors = {
        log: chalk.white,
        error: chalk.red,
        warning: chalk.yellow,
        info: chalk.cyan,
    }
    type SupportedTypes = keyof typeof messageColors
    function isSupportedType(messageType: ConsoleMessageType): messageType is SupportedTypes {
        return messageType in messageColors
    }
    const messageType = message.type()
    const color = isSupportedType(messageType) ? messageColors[messageType] : chalk.white
    // eslint-disable-next-line no-console
    console.log(color(`${message.type()} --- ${JSON.stringify(message.location())} --- ${message.text()}`))
}

const redirectError = (error: Error) => {
    console.error(error)
}
