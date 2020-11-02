import { useToast, UseToastOptions } from "@chakra-ui/core"
import React, { useEffect } from "react"
import { connect } from "react-redux"
import { Dispatch } from "redux"
import AlertBox from "../../../application/components/common/AlertBox"
import PageLoader from "../../../application/components/common/PageLoader"
import { AppState } from "../../../application/store"
import { OperationStatus } from "../../../application/store/async/AsyncOperationResult"
import { fetchMembersAction } from "../../game/store/Actions"
import { Game } from "../../game/types/Game"
import { fetchMeInfoAction } from "../../user/store/Actions"
import { fetchMatchStateAction, resetMatchStateAction } from "../store/Actions"
import { startMatchWsConnectionAction, stopMatchWsConnectionAction } from "../store/websocket/Actions"
import MatchView from "./MatchView"

type Props = {
    game: Game
} & ReturnType<typeof mapStateToProps> &
    ReturnType<typeof mapDispatchToProps>

const MatchContainer: React.FC<Props> = ({
    game,
    matchState,
    fetchMatchState,
    resetMatchState,
    fetchCurrentUser,
    session,
    members,
    fetchGameMembers,
    connectMatchWebSocket,
    disconnectMatchWebSocket,
    playerTakesTrick,
    scores,
}) => {
    const toast = useToast()

    useEffect(() => {
        if (
            matchState.params !== game.id ||
            ![OperationStatus.PENDING, OperationStatus.FINISHED].includes(matchState.status)
        ) {
            fetchMatchState(game.id)
        }
        return () => {
            resetMatchState()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    useEffect(() => {
        if (!session.data && session.status !== OperationStatus.PENDING) {
            fetchCurrentUser()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    useEffect(() => {
        if (members.params !== game.id || members.status !== OperationStatus.PENDING) {
            fetchGameMembers(game.id)
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    useEffect(() => {
        connectMatchWebSocket(game.id)
        return () => {
            disconnectMatchWebSocket(game.id)
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    useEffect(() => {
        const toastOptions: UseToastOptions = { isClosable: true, duration: 7000, position: "top" }
        if (playerTakesTrick && playerTakesTrick.player) {
            const winnerName = members.data?.find((p) => p.id === playerTakesTrick.player)?.nickname
            if (winnerName) {
                toast({ title: `${winnerName} takes a trick`, ...toastOptions })
            }
        } else if (playerTakesTrick && !playerTakesTrick.player) {
            toast({ title: "Nobody managed to take a trick", ...toastOptions })
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [playerTakesTrick])

    if (matchState.error) {
        return <AlertBox title="Unable to read match status" description={matchState.error.message} status="error" />
    } else if (session.error) {
        return <AlertBox title="Unable to read users data" description={session.error.message} status="error" />
    } else if (members.error) {
        return <AlertBox title="Unable to read game members" description={members.error.message} status="error" />
    } else if (matchState.data && session.data && members.data) {
        return (
            <MatchView game={game} round={matchState.data} user={session.data} members={members.data} scores={scores} />
        )
    } else {
        return <PageLoader text="Loading match data" />
    }
}

const mapStateToProps = (state: AppState) => ({
    matchState: state.matches.matchState,
    session: state.users.current,
    members: state.games.members,
    playerTakesTrick: state.matches.playerTakesTrick,
    scores: state.matches.scores,
})

const mapDispatchToProps = (dispatch: Dispatch) => ({
    fetchMatchState: (matchId: string) => dispatch(fetchMatchStateAction.started(matchId)),
    resetMatchState: () => dispatch(resetMatchStateAction()),
    fetchCurrentUser: () => dispatch(fetchMeInfoAction.started()),
    fetchGameMembers: (gameId: string) => dispatch(fetchMembersAction.started(gameId)),
    connectMatchWebSocket: (gameId: string) => dispatch(startMatchWsConnectionAction(gameId)),
    disconnectMatchWebSocket: (gameId: string) => dispatch(stopMatchWsConnectionAction(gameId)),
})

export default connect(mapStateToProps, mapDispatchToProps)(MatchContainer)
