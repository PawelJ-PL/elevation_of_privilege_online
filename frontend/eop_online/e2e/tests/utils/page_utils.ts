import { ElementHandle, Page, ScreenshotOptions, WaitForSelectorOptions } from "puppeteer"
import snakeCase from "lodash/snakeCase"
import fs from "fs"

const screenshotsBasePath = "e2e/screenshots/"

export const liftedScreenshotOptions = (screenshotOptions: ScreenshotOptions, pathSuffix?: string) => {
    const testname = expect?.getState()?.currentTestName
    const screenshotDir = testname
        ? screenshotsBasePath + snakeCase(testname) + (pathSuffix ?? "") + "/"
        : screenshotsBasePath
    const fullPath =
        screenshotOptions.path !== undefined ? screenshotDir + `${Date.now()}_` + screenshotOptions.path : undefined
    if (fullPath && !fs.existsSync(screenshotDir)) {
        fs.mkdirSync(screenshotDir, { recursive: true })
    }
    return { ...screenshotOptions, path: fullPath, fullPage: screenshotOptions.fullPage ?? true }
}

const takeScreenshotAndReturnError = (page: Page, error: unknown, screenshotOptions?: ScreenshotOptions) => {
    if (!screenshotOptions) {
        return Promise.reject(error)
    } else {
        const liftedOptions = liftedScreenshotOptions(screenshotOptions ?? {}, "_error")
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

export const getAllByTestId = async (
    page: Page,
    testId: string,
    waitOptions?: WaitForSelectorOptions,
    screenshotOptions?: ScreenshotOptions
) => {
    if (waitOptions) {
        await waitForTestId(page, testId, waitOptions, screenshotOptions)
    }
    return await page
        .$$(`[data-testid='${testId}']`)
        .catch((e) => takeScreenshotAndReturnError(page, e, screenshotOptions))
}

export const getByRoleFrom = async (element: ElementHandle<Element>, role: string) => {
    const selector = `[role='${role}']`
    const result = await element.$(selector)
    return !result
        ? Promise.reject(new Error(`Unable to find element with role ${role} (selector ${selector})`))
        : Promise.resolve(result)
}

export const getAllByRoleFrom = (element: ElementHandle<Element>, role: string) => {
    const selector = `[role='${role}']`
    return element.$$(selector)
}

export const getByTextAndTagFrom = async (element: ElementHandle<Element>, tagName: string, text: string) => {
    const result = await getAllByTextAndTagFrom(element, tagName, text)
    if (result.length < 1) {
        return Promise.reject(new Error(`Unable to find tag ${tagName} with text ${text}`))
    } else if (result.length > 1) {
        return Promise.reject(new Error(`Found more than one tag ${tagName} with text ${text}`))
    } else {
        return Promise.resolve(result[0])
    }
}

export const getAllByTextAndTagFrom = (element: ElementHandle<Element>, tagName: string, text: string) => {
    const xpath = `//${tagName}[text()='${text}']`
    return element.$x(xpath)
}

export const getAllByTestIdFrom = (element: ElementHandle<Element>, testId: string) => {
    const selector = `[data-testid='${testId}']`
    return element.$$(selector)
}
