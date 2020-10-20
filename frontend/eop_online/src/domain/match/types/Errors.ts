export class MatchNotFound extends Error {
    constructor(message: string) {
        super(message)
        Object.setPrototypeOf(this, MatchNotFound.prototype)
        this.name = "MatchNotFound"
    }
}
