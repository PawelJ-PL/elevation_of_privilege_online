import { ElementHandle, Page, ScreenshotOptions, WaitForSelectorOptions } from "puppeteer"

const screenshotsBasePath = "e2e/screenshots/"

const takeScreenshotAndReturnError = (page: Page, error: unknown, screenshotOptions?: ScreenshotOptions) => {
    if (!screenshotOptions) {
        return Promise.reject(error)
    } else {
        const fullPath =
            screenshotOptions.path !== undefined
                ? screenshotsBasePath + `${Date.now()}_` + screenshotOptions.path
                : undefined
        const liftedOptions = { ...screenshotOptions, path: fullPath }
        return page.screenshot(liftedOptions).then(() => Promise.reject(error))
    }
}

export const waitForXpath = (
    page: Page,
    xpath: string,
    waitOptions?: WaitForSelectorOptions,
    screenshotOptions?: ScreenshotOptions
) => page.waitForXPath(xpath, waitOptions).catch((e) => takeScreenshotAndReturnError(page, e, screenshotOptions))

export const waitForElementWithText = (
    page: Page,
    elementName: string,
    textContent: string,
    waitOptions: WaitForSelectorOptions,
    screenshotOptions?: ScreenshotOptions
) => {
    const xpath = `//${elementName}[text()='${textContent}']`
    return waitForXpath(page, xpath, waitOptions, screenshotOptions)
}

export const waitForSelector = (
    page: Page,
    selector: string,
    waitOptions?: WaitForSelectorOptions,
    screenshotOptions?: ScreenshotOptions
) => page.waitForSelector(selector, waitOptions).catch((e) => takeScreenshotAndReturnError(page, e, screenshotOptions))

export const waitForTestId = (
    page: Page,
    testId: string,
    waitOptions?: WaitForSelectorOptions,
    screenshotOptions?: ScreenshotOptions
) => {
    const selector = `[data-testid='${testId}']`
    return waitForSelector(page, selector, waitOptions, screenshotOptions)
}

export const getTextOfAllChildren = (container: ElementHandle, trim?: boolean) =>
    container.evaluate((box, shouldTrim) => {
        const texts = Array.from(box.children).map((c) => c.textContent)
        return shouldTrim === false ? texts : texts.map((t) => t?.trim())
    }, trim ?? true)
