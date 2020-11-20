import { Member } from "../../../domain/game/types/Member"

export const members: Member[] = [
    {
        id: "111",
        nickname: "foo",
        role: "Player",
    },
    {
        id: "222",
        nickname: "bar",
        role: "Observer",
    },
    {
        id: "333",
        nickname: "baz",
        role: "Player",
    },
    {
        id: "444",
        nickname: "qux",
        role: "Observer",
    },
    {
        id: "555",
        nickname: "quux",
        role: "Player",
    },
    {
        id: "666",
        nickname: "quuz",
        role: "Player",
    },
]

export const scores = {
    "555": 10,
    "111": 8,
    "333": 15,
    "666": 5,
}

export const game = { id: "foo-bar", description: null, creator: "baz", startedAt: null, finishedAt: null }
