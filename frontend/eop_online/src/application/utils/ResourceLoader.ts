import { Card, Suit } from "./../../domain/match/types/Card"
import kebabCase from "lodash/kebabCase"

export function loadSuitLogo(suit: Suit): string | undefined {
    const context = require.context("../../resources/suites", false, /\.png$/)
    try {
        return context(`./${kebabCase(suit)}.png`)
    } catch (_) {
        return undefined
    }
}

export function loadCardImage(card: Card): string | undefined {
    const filename = kebabCase(card.suit + card.value)
    return process.env.PUBLIC_URL + `/cards/${filename}.png`
}
