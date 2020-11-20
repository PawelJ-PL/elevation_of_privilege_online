import { Page, Request } from "puppeteer"
import { exampleGame, initMember, meData } from "../constants"

export type RequestHandler = {
    reqPredicate: (req: Request, url: URL) => boolean
    onMatch: (req: Request, url: URL) => Promise<unknown>
}

export type RequestHandlersOptions = {
    onlyApiRequests?: boolean
    handlers?: RequestHandler[]
    unmatchedHandler?: RequestHandler
}

const defaultUnmatchedHandler = (req: Request) => {
    console.warn(`Request ${req.method()} to ${req.url()} not handled`)
    return req.abort()
}

export const setRequestHandlers = async (page: Page, options?: RequestHandlersOptions) => {
    if (process.env.INTERCEPT_REQUESTS?.toLowerCase() === "false") {
        return Promise.resolve()
    } else {
        await page.setRequestInterception(true)
        page.on("request", (request) => {
            const url = new URL(request.url())
            const handlers = options?.handlers ?? []
            if (options?.onlyApiRequests !== false && !url.pathname.startsWith("/api/v1")) {
                request.continue()
            } else {
                const matchingHandler =
                    handlers.find((h) => h.reqPredicate(request, url))?.onMatch ?? defaultUnmatchedHandler
                matchingHandler(request, url)
            }
        })
    }
}

const asHandlerTypes = <T>(ht: { [K in keyof T]: RequestHandler }) => ht

export const matchers = asHandlerTypes({
    createGameSuccess: {
        reqPredicate: (r, u) => u.pathname === "/api/v1/games" && r.method() === "POST",
        onMatch: (r) => r.respond({ body: JSON.stringify(exampleGame) }),
    },
    getDefaultGame: {
        reqPredicate: (r, u) => u.pathname === "/api/v1/games/" + exampleGame.id && r.method() === "GET",
        onMatch: (r) => r.respond({ body: JSON.stringify(exampleGame) }),
    },
    getMeData: {
        reqPredicate: (r, u) => u.pathname === "/api/v1/users/me" && r.method() === "GET",
        onMatch: (r) => r.respond({ body: JSON.stringify(meData) }),
    },
    getInitMembers: {
        reqPredicate: (r, u) => u.pathname === `/api/v1/games/${exampleGame.id}/members` && r.method() === "GET",
        onMatch: (r) => r.respond({ body: JSON.stringify([initMember]) }),
    },
})
