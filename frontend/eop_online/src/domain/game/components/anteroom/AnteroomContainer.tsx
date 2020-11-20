import React, { useEffect } from "react"
import { connect } from "react-redux"
import { Dispatch } from "redux"
import AlertBox from "../../../../application/components/common/AlertBox"
import PageLoader from "../../../../application/components/common/PageLoader"
import { AppState } from "../../../../application/store"
import { OperationStatus } from "../../../../application/store/async/AsyncOperationResult"
import { fetchMeInfoAction } from "../../../user/store/Actions"
import { fetchMembersAction } from "../../store/Actions"
import { Game } from "../../types/Game"
import AnteroomView from "./AnteroomView"

type Props = {
    game: Game
} & ReturnType<typeof mapStateToProps> &
    ReturnType<typeof mapDispatchToProps>

export const AnteroomContainer: React.FC<Props> = ({ game, currentUser, fetchUserData, members, fetchGameMembers }) => {
    useEffect(() => {
        if (!currentUser.data && currentUser.status !== OperationStatus.PENDING) {
            fetchUserData()
        }
        fetchGameMembers(game.id)
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    if ([OperationStatus.NOT_STARTED, OperationStatus.PENDING].includes(currentUser.status)) {
        return <PageLoader text="Loading user data" />
    } else if (currentUser.status === OperationStatus.FAILED) {
        return <AlertBox title="Unable to load users data" description={currentUser.error?.message} status="error" />
    } else if ([OperationStatus.NOT_STARTED, OperationStatus.PENDING].includes(members.status)) {
        return <PageLoader text="Loading game members" />
    } else if (members.status === OperationStatus.FAILED) {
        return <AlertBox title="Unable to load game members" description={members.error?.message} status="error" />
    } else if (members.data && currentUser.data) {
        return <AnteroomView game={game} currentUser={currentUser.data} members={members.data} />
    } else {
        return <AlertBox title="Something went wrong" status="error" />
    }
}

const mapStateToProps = (state: AppState) => ({
    currentUser: state.users.current,
    members: state.games.members,
})

const mapDispatchToProps = (dispatch: Dispatch) => ({
    fetchUserData: () => dispatch(fetchMeInfoAction.started()),
    fetchGameMembers: (gameId: string) => dispatch(fetchMembersAction.started(gameId)),
})

export default connect(mapStateToProps, mapDispatchToProps)(AnteroomContainer)
