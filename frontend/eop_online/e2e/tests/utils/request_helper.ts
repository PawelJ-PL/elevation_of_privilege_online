import WebSocket from "ws"
import {
    exampleRound,
    exampleUsersGame1,
    exampleUsersGame2,
    exampleUsersGame3,
    observer1,
    observer1Id,
    player2,
    player3,
} from "./../constants"
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

const asPredicateTypes = <T>(pt: { [K in keyof T]: (req: Request, url: URL) => boolean }) => pt

export const requestPredicates = asPredicateTypes({
    createNewGame: (r, u) => u.pathname === "/api/v1/games" && r.method() === "POST",
    getDefaultGameData: (r, u) => u.pathname === "/api/v1/games/" + exampleGame.id && r.method() === "GET",
    getCurrentUser: (r, u) => u.pathname === "/api/v1/users/me" && r.method() === "GET",
    getDefaultGameMembers: (r, u) => u.pathname === `/api/v1/games/${exampleGame.id}/members` && r.method() === "GET",
    getDefaultMatchState: (r, u) => u.pathname === `/api/v1/matches/${exampleGame.id}` && r.method() === "GET",
    getDefaultScores: (r, u) => u.pathname === `/api/v1/matches/${exampleGame.id}/scores` && r.method() === "GET",
    listAvailableGames: (r, u) => u.pathname === "/api/v1/games" && r.method() === "GET",
    deleteDefaultGame: (r, u) => u.pathname === `/api/v1/games/${exampleGame.id}` && r.method() === "DELETE",
})

const asHandlerTypes = <T>(ht: { [K in keyof T]: RequestHandler }) => ht

export const matchers = asHandlerTypes({
    createGameSuccess: {
        reqPredicate: requestPredicates.createNewGame,
        onMatch: (r) => r.respond({ body: JSON.stringify(exampleGame) }),
    },
    getDefaultGame: {
        reqPredicate: requestPredicates.getDefaultGameData,
        onMatch: (r) => r.respond({ body: JSON.stringify(exampleGame) }),
    },
    getMeData: {
        reqPredicate: requestPredicates.getCurrentUser,
        onMatch: (r) => r.respond({ body: JSON.stringify(meData) }),
    },
    getInitMembers: {
        reqPredicate: requestPredicates.getDefaultGameMembers,
        onMatch: (r) => r.respond({ body: JSON.stringify([initMember]) }),
    },
    getMembers: {
        reqPredicate: requestPredicates.getDefaultGameMembers,
        onMatch: (r) => r.respond({ body: JSON.stringify([initMember, player2, player3, observer1]) }),
    },
    getGameMembers: {
        reqPredicate: requestPredicates.getDefaultGameMembers,
        onMatch: (r) => r.respond({ body: JSON.stringify([initMember, player2, player3, observer1Id]) }),
    },
    getDefaultMatch: {
        reqPredicate: requestPredicates.getDefaultMatchState,
        onMatch: (r) => r.respond({ body: JSON.stringify(exampleRound) }),
    },
    getDefaultScores: {
        reqPredicate: requestPredicates.getDefaultScores,
        onMatch: (r) => r.respond({ body: JSON.stringify(exampleRound.playersScores) }),
    },
    listDefaultGames: {
        reqPredicate: requestPredicates.listAvailableGames,
        onMatch: (r) => r.respond({ body: JSON.stringify([exampleUsersGame1, exampleUsersGame2, exampleUsersGame3]) }),
    },
    deleteDefaultGame: {
        reqPredicate: requestPredicates.deleteDefaultGame,
        onMatch: (r) => r.respond({ body: "", status: 201 }),
    },
})

export const matcherCreators = {
    deleteMember(memberId: string, gameId?: string, wsClients?: Set<WebSocket>): RequestHandler {
        const game = gameId ?? exampleGame.id
        const clients = wsClients ?? new Set()
        return {
            reqPredicate: (r, u) =>
                u.pathname === `/api/v1/games/${game}/members/${memberId}` && r.method() === "DELETE",
            onMatch: (r) => {
                clients.forEach((c) =>
                    c.send(
                        JSON.stringify({
                            eventType: "ParticipantRemoved",
                            payload: { gameId: game, userId: memberId },
                        })
                    )
                )
                return r.respond({})
            },
        }
    },
    setRole(memberId: string, newRole: string, gameId?: string, wsClients?: Set<WebSocket>): RequestHandler {
        const game = gameId ?? exampleGame.id
        const clients = wsClients ?? new Set()
        return {
            reqPredicate: (r, u) =>
                u.pathname === `/api/v1/games/${game}/members/${memberId}/roles/${newRole}` && r.method() === "PUT",
            onMatch: (r) => {
                clients.forEach((c) =>
                    c.send(
                        JSON.stringify({
                            eventType: "UserRoleChanged",
                            payload: { gameId: game, userId: memberId, role: newRole },
                        })
                    )
                )
                return r.respond({})
            },
        }
    },
}
