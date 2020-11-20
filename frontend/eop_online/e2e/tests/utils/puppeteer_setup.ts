import { Browser } from "puppeteer"

declare global {
    const __BROWSER__: Browser
}

export const setupPuppeteer = async () => {
    const browser = __BROWSER__
    const page = await browser.newPage()
    return { page }
}
