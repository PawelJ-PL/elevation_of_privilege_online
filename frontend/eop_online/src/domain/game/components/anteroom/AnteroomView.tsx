import { Box, Button, Flex, Spacer } from "@chakra-ui/core"
import React, { useEffect } from "react"
import ContentBox from "../../../../application/components/common/ContentBox"
import { Session } from "../../../user/types/Session"
import { Game } from "../../types/Game"
import { Member } from "../../types/Member"
import groupBy from "lodash/groupBy"
import ElementsList from "../../../../application/components/common/ElementsList"
import { AppState } from "../../../../application/store"
import { Dispatch } from "redux"
import { connect } from "react-redux"
import {
    resetAssignRoleStatusAction,
    resetKickUserStatusAction,
    resetStartGameStatusAction,
    startGameAction,
} from "../../store/Actions"
import LoadingDimmer from "../../../../application/components/common/LoadingDimmer"
import { OperationStatus } from "../../../../application/store/async/AsyncOperationResult"
import AlertBox from "../../../../application/components/common/AlertBox"
import RoleEntry from "./RoleEntry"

type Props = {
    game: Game
    currentUser: Session
    members: Member[]
} & ReturnType<typeof mapStateToProps> &
    ReturnType<typeof mapDispatchToProps>

const AnteroomView: React.FC<Props> = ({
    game,
    members,
    currentUser,
    resetAssignRoleStatus,
    assignRoleStatus,
    resetKickStatus,
    kickUserStatus,
    startGameStatus,
    startGame,
    resetStartGameStatus,
}) => {
    const playerRoles = groupBy(members, (m) => (!m.role ? "None" : m.role))
    const isAdmin = currentUser.userId === game.creator

    useEffect(() => {
        return () => {
            resetAssignRoleStatus()
            resetKickStatus()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    return (
        <ContentBox
            title="New Elevation of Privilege game"
            description={game.description ?? undefined}
            containerProps={{ "data-testid": "anteroom-container" }}
        >
            <LoadingDimmer
                active={[assignRoleStatus.status, kickUserStatus.status].includes(OperationStatus.PENDING)}
            />
            {assignRoleStatus.status === OperationStatus.FAILED && (
                <AlertBox
                    title="Unable to change users role"
                    containerProps={{ marginTop: "0.5em" }}
                    description={assignRoleStatus.error?.message}
                    status="error"
                    variant="solid"
                    onClose={() => resetAssignRoleStatus()}
                />
            )}
            {kickUserStatus.status === OperationStatus.FAILED && (
                <AlertBox
                    title="Unable to kick user"
                    containerProps={{ marginTop: "0.5em" }}
                    description={kickUserStatus.error?.message}
                    status="error"
                    variant="solid"
                    onClose={() => resetKickStatus()}
                />
            )}
            {startGameStatus.status === OperationStatus.FAILED && (
                <AlertBox
                    title="Error during starting game"
                    containerProps={{ marginTop: "0.5em" }}
                    description={startGameStatus.error?.message}
                    status="error"
                    variant="solid"
                    onClose={resetStartGameStatus}
                />
            )}
            <Flex direction={["column", "column", "row"]} marginTop="0.5em" marginX="0.5em">
                <ElementsList
                    elements={(playerRoles["None"] || []).map((m) => (
                        <RoleEntry
                            key={m.id}
                            gameId={game.id}
                            member={m}
                            currentPlayerId={currentUser.userId}
                            ownerId={game.creator}
                            hideAddPlayer={!isAdmin}
                            hideAddObserver={!isAdmin}
                            hideKick={!isAdmin}
                        />
                    ))}
                    title="Waiting for approval"
                    containerProps={{ marginBottom: ["0.5em", "0.5em", "0"] }}
                />
                <Spacer />
                <ElementsList
                    elements={(playerRoles["Player"] || []).map((m) => (
                        <RoleEntry
                            key={m.id}
                            gameId={game.id}
                            member={m}
                            hideAddPlayer={true}
                            hideAddObserver={!isAdmin}
                            hideKick={!isAdmin}
                            currentPlayerId={currentUser.userId}
                            ownerId={game.creator}
                        />
                    ))}
                    title="Accepted players"
                    containerProps={{ marginBottom: ["0.5em", "0.5em", "0"] }}
                />
                <Spacer />
                <ElementsList
                    elements={(playerRoles["Observer"] || []).map((m) => (
                        <RoleEntry
                            key={m.id}
                            gameId={game.id}
                            member={m}
                            hideAddPlayer={!isAdmin}
                            hideAddObserver={true}
                            hideKick={!isAdmin}
                            currentPlayerId={currentUser.userId}
                            ownerId={game.creator}
                        />
                    ))}
                    title="Accepted observers"
                />
            </Flex>
            {isAdmin && (
                <Box marginTop="0.5em" marginX="0.5em">
                    <Button
                        width="100%"
                        colorScheme="blue"
                        disabled={(playerRoles["Player"] ?? []).length < 2 || (playerRoles["Player"] ?? []).length > 10}
                        onClick={() => startGame(game.id)}
                    >
                        Start game
                    </Button>
                </Box>
            )}
        </ContentBox>
    )
}

const mapStateToProps = (state: AppState) => ({
    assignRoleStatus: state.games.assignRoleStatus,
    kickUserStatus: state.games.kickUserStatus,
    startGameStatus: state.games.startGame,
})

const mapDispatchToProps = (dispatch: Dispatch) => ({
    resetAssignRoleStatus: () => dispatch(resetAssignRoleStatusAction()),
    resetKickStatus: () => dispatch(resetKickUserStatusAction()),
    startGame: (gameId: string) => dispatch(startGameAction.started(gameId)),
    resetStartGameStatus: () => dispatch(resetStartGameStatusAction()),
})

export default connect(mapStateToProps, mapDispatchToProps)(AnteroomView)
