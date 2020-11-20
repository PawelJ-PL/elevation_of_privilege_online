import { UserRoleChanged } from "./../types/Events"
import { Member } from "./../types/Member"
import { OperationStatus } from "./../../../application/store/async/AsyncOperationResult"
import { newParticipantAction, userRemovedAction, userRoleChangedAction } from "./Actions"
import { gamesReducer } from "./Reducers"
const defaultState = {} as ReturnType<typeof gamesReducer>

describe("Game reducers", () => {
    describe("Members", () => {
        const members: Member[] = [
            { id: "111", nickname: "A", role: "Player" },
            { id: "222", nickname: "B", role: "Observer" },
            { id: "333", nickname: "C" },
        ]
        const membersData = { status: OperationStatus.FINISHED, params: "foo-bar", data: members }

        describe("On user removed action", () => {
            it("should be updated", () => {
                const state = { ...defaultState, members: membersData }
                const action = userRemovedAction({ gameId: "foo-bar", userId: "222" })
                const result = gamesReducer(state, action)
                expect(result.members.data).toEqual([members[0], members[2]])
            })

            it("should not be updated if current status is not FINISHED", () => {
                const state = { ...defaultState, members: { ...membersData, status: OperationStatus.PENDING } }
                const action = userRemovedAction({ gameId: "foo-bar", userId: "222" })
                const result = gamesReducer(state, action)
                expect(result.members.data).toEqual(members)
            })

            it("should not be updated if gameId is different", () => {
                const state = { ...defaultState, members: membersData }
                const action = userRemovedAction({ gameId: "other-game", userId: "222" })
                const result = gamesReducer(state, action)
                expect(result.members.data).toEqual(members)
            })
        })

        describe("On user added", () => {
            const newPlayer = { id: "444", nickname: "D" }

            it("should be updated", () => {
                const state = { ...defaultState, members: membersData }
                const action = newParticipantAction({
                    gameId: "foo-bar",
                    userId: newPlayer.id,
                    nickName: newPlayer.nickname,
                })
                const result = gamesReducer(state, action)
                expect(result.members.data).toEqual(members.concat(newPlayer))
            })

            it("should not be updated if current status is not FINISHED", () => {
                const state = { ...defaultState, members: { ...membersData, status: OperationStatus.PENDING } }
                const action = newParticipantAction({
                    gameId: "foo-bar",
                    userId: newPlayer.id,
                    nickName: newPlayer.nickname,
                })
                const result = gamesReducer(state, action)
                expect(result.members.data).toEqual(members)
            })

            it("should not be updated if gameId is different", () => {
                const state = { ...defaultState, members: membersData }
                const action = newParticipantAction({
                    gameId: "other-game",
                    userId: newPlayer.id,
                    nickName: newPlayer.nickname,
                })
                const result = gamesReducer(state, action)
                expect(result.members.data).toEqual(members)
            })
        })

        describe("On role changed", () => {
            const updatedUser: UserRoleChanged = { gameId: "foo-bar", userId: "222", role: "Player" }

            it("should be updated", () => {
                const state = { ...defaultState, members: membersData }
                const action = userRoleChangedAction(updatedUser)
                const result = gamesReducer(state, action)
                expect(result.members.data).toEqual([members[0], { ...members[1], role: "Player" }, members[2]])
            })

            it("should be updated is role was not assigned", () => {
                const state = { ...defaultState, members: membersData }
                const action = userRoleChangedAction({ ...updatedUser, userId: "333" })
                const result = gamesReducer(state, action)
                expect(result.members.data).toEqual([members[0], members[1], { ...members[2], role: "Player" }])
            })

            it("should not be updated if user does not exist", () => {
                const state = { ...defaultState, members: membersData }
                const action = userRoleChangedAction({ ...updatedUser, userId: "999" })
                const result = gamesReducer(state, action)
                expect(result.members.data).toEqual(members)
            })

            it("should not be updated if current status is not FINISHED", () => {
                const state = { ...defaultState, members: { ...membersData, status: OperationStatus.PENDING } }
                const action = userRoleChangedAction(updatedUser)
                const result = gamesReducer(state, action)
                expect(result.members.data).toEqual(members)
            })

            it("should not be updated if current game has different id", () => {
                const state = { ...defaultState, members: membersData }
                const action = userRoleChangedAction({ ...updatedUser, gameId: "other-game" })
                const result = gamesReducer(state, action)
                expect(result.members.data).toEqual(members)
            })
        })
    })
})
