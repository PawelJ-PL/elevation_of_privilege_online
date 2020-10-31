export class MatchNotFound extends Error {
    constructor(message: string) {
        super(message)
        Object.setPrototypeOf(this, MatchNotFound.prototype)
        this.name = "MatchNotFound"
    }
}

export class NotAPlayer extends Error {
    constructor(message: string) {
        super(message)
        Object.setPrototypeOf(this, NotAPlayer.prototype)
        this.name = "NotAPlayer"
    }
}

export class OtherPlayersTurn extends Error {
    constructor(message: string) {
        super(message)
        Object.setPrototypeOf(this, OtherPlayersTurn.prototype)
        this.name = "OtherPlayersTurn"
    }
}

export class CardNotFound extends Error {
    constructor(message: string) {
        super(message)
        Object.setPrototypeOf(this, CardNotFound.prototype)
        this.name = "CardNotFound"
    }
}

export class OtherPlayersCard extends Error {
    constructor(message: string) {
        super(message)
        Object.setPrototypeOf(this, OtherPlayersCard.prototype)
        this.name = "OtherPlayersCard"
    }
}

export class CardNotOnTable extends Error {
    constructor(message: string) {
        super(message)
        Object.setPrototypeOf(this, CardNotOnTable.prototype)
        this.name = "CardNotOnTable"
    }
}

export class ThreatStatusAlreadyAssigned extends Error {
    constructor(message: string) {
        super(message)
        Object.setPrototypeOf(this, ThreatStatusAlreadyAssigned.prototype)
        this.name = "ThreatStatusAlreadyAssigned"
    }
}

export class CardNotOnTheHand extends Error {
    constructor(message: string) {
        super(message)
        Object.setPrototypeOf(this, CardNotOnTheHand.prototype)
        this.name = "CardNotOnTheHand"
    }
}

export class SuitNotMatch extends Error {
    constructor(message: string) {
        super(message)
        Object.setPrototypeOf(this, SuitNotMatch.prototype)
        this.name = "SuitNotMatch"
    }
}

export class PlayerAlreadyPlayedCard extends Error {
    constructor(message: string) {
        super(message)
        Object.setPrototypeOf(this, PlayerAlreadyPlayedCard.prototype)
        this.name = "PlayerAlreadyPlayedCard"
    }
}
