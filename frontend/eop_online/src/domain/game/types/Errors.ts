export class UserIsNotGameMember extends Error {
    constructor(message: string) {
        super(message)
        Object.setPrototypeOf(this, UserIsNotGameMember.prototype)
        this.name = "UserIsNotGameMember"
    }
}

export class UserNotAccepted extends Error {
    constructor(message: string) {
        super(message)
        Object.setPrototypeOf(this, UserNotAccepted.prototype)
        this.name = "UserNotAccepted"
    }
}

export class GameNotFound extends Error {
    constructor(message: string) {
        super(message)
        Object.setPrototypeOf(this, GameNotFound.prototype)
        this.name = "GameNotFound"
    }
}

export class UserAlreadyJoined extends Error {
    constructor() {
        super("User already joined")
        Object.setPrototypeOf(this, UserAlreadyJoined.prototype)
        this.name = "UserAlreadyJoined"
    }
}

export class GameAlreadyStarted extends Error {
    constructor(message: string) {
        super(message)
        Object.setPrototypeOf(this, GameAlreadyStarted.prototype)
        this.name = "GameAlreadyStarted"
    }
}

export class UserRemoved extends Error {
    constructor(message: string) {
        super(message)
        Object.setPrototypeOf(this, UserRemoved.prototype)
        this.name = "UserRemoved"
    }
}

export class UserIsNotGameOwner extends Error {
    constructor(message: string) {
        super(message)
        Object.setPrototypeOf(this, UserIsNotGameOwner.prototype)
        this.name = "UserIsNotGameOwner"
    }
}

export class NotEnoughPlayers extends Error {
    constructor(message: string) {
        super(message)
        Object.setPrototypeOf(this, NotEnoughPlayers.prototype)
        this.name = "NotEnoughPlayers"
    }
}

export class TooManyPlayers extends Error {
    constructor(message: string) {
        super(message)
        Object.setPrototypeOf(this, TooManyPlayers.prototype)
        this.name = "TooManyPlayers"
    }
}

export class GameAlreadyFinished extends Error {
    constructor(message: string) {
        super(message)
        Object.setPrototypeOf(this, GameAlreadyFinished.prototype)
        this.name = "GameAlreadyFinished"
    }
}
