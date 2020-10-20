import { Card, Suit } from './../../domain/match/types/Card';
import kebabCase from "lodash/kebabCase"

export function loadSuitLogo(suit: Suit): string | undefined {
    try {
        return require(`../../resources/suites/${kebabCase(suit)}.png`)
    } catch (_) {
        return undefined
    }
}

export function loadCardImage(card: Card): string | undefined {
    const filename = kebabCase(card.suit + card.value)
    try {
        return require(`../../resources/cards/${filename}.png`)
    } catch (_) {
        return undefined
    }
}