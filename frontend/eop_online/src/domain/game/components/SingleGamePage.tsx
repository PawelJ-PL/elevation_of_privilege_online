import { useToast } from "@chakra-ui/core"
import React, { useEffect } from "react"
import { connect } from "react-redux"
import { RouteComponentProps, useHistory, withRouter } from "react-router-dom"
import { Dispatch } from "redux"
import AlertBox from "../../../application/components/common/AlertBox"
import PageLoader from "../../../application/components/common/PageLoader"
import { AppState } from "../../../application/store"
import { OperationStatus } from "../../../application/store/async/AsyncOperationResult"
import MatchContainer from "../../match/components/MatchContainer"
import { fetchGameInfoAction, resetGameInfoStatusAction } from "../store/Actions"
import { startAnteroomWsConnectionAction, stopAnteroomWsConnectionAction } from "../store/websocket/Actions"
import { UserIsNotGameMember, UserNotAccepted, UserRemoved } from "../types/Errors"
import AfterGameSummary from "./AfterGameSummary"
import AnteroomContainer from "./anteroom/AnteroomContainer"
import JoinGameModal from "./JoinGameModal"

type Props = ReturnType<typeof mapStateToProps> &
    ReturnType<typeof mapDispatchToProps> &
    RouteComponentProps<{ gameId: string }>

const SingleGamePage: React.FC<Props> = ({
    match,
    fetchGameInfo,
    gameInfo,
    startAnteroomWs,
    stopAnteroomWs,
    resetGameStatus,
}) => {
    const gameId = match.params.gameId
    const isCurrentGame = gameInfo.params === gameId

    const toast = useToast()
    const history = useHistory()

    useEffect(() => {
        fetchGameInfo(gameId)
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    useEffect(() => {
        if (gameInfo.status === OperationStatus.FINISHED && isCurrentGame && gameInfo.data === null) {
            toast({ title: "Game not found", position: "top", duration: 3000, status: "warning" })
            history.push("/")
        } else if (gameInfo.error instanceof UserRemoved) {
            toast({ title: "User removed from game", position: "top", duration: 3000, status: "warning" })
            history.push("/")
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [gameInfo, isCurrentGame])

    useEffect(() => {
        if ((gameInfo.data && !gameInfo.data.startedAt) || gameInfo.error instanceof UserNotAccepted) {
            startAnteroomWs(gameId)
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [gameInfo, gameId])

    useEffect(() => {
        if (Boolean(gameInfo.data?.startedAt) && !gameInfo.data?.finishedAt) {
            stopAnteroomWs(gameId)
        }
         // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [gameInfo, gameId])

    useEffect(() => {
        return () => {
            stopAnteroomWs(gameId)
            resetGameStatus()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    if (!isCurrentGame) {
        return <PageLoader text="Loading game" />
    } else if (gameInfo.error instanceof UserNotAccepted) {
        return <PageLoader text="Waiting for approval" />
    } else if (gameInfo.error instanceof UserIsNotGameMember) {
        return <JoinGameModal gameId={gameId} isOpen={true} onClose={() => void 0} />
    } else if (gameInfo.error) {
        return <AlertBox status="error" title="Unable to load game data" description={gameInfo.error?.message} />
    } else if (gameInfo.data?.finishedAt) {
        return <AfterGameSummary game={gameInfo.data} />
    } else if (gameInfo.data?.startedAt && !gameInfo.data?.finishedAt) {
        return <MatchContainer game={gameInfo.data} />
    } else if (gameInfo.data && !gameInfo.data.startedAt) {
        return <AnteroomContainer game={gameInfo.data} />
    } else {
        return <PageLoader text="Loading game" />
    }
}

const mapStateToProps = (state: AppState) => ({
    gameInfo: state.games.fetchStatus,
})

const mapDispatchToProps = (dispatch: Dispatch) => ({
    fetchGameInfo: (gameId: string) => dispatch(fetchGameInfoAction.started(gameId)),
    startAnteroomWs: (gameId: string) => dispatch(startAnteroomWsConnectionAction(gameId)),
    stopAnteroomWs: (gameId: string) => dispatch(stopAnteroomWsConnectionAction(gameId)),
    resetGameStatus: () => dispatch(resetGameInfoStatusAction()),
})

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(SingleGamePage))
