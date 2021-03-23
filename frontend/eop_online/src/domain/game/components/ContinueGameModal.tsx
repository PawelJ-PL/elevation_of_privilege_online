import {
    Modal,
    ModalBody,
    ModalContent,
    ModalOverlay,
    Tooltip,
    Text,
    IconButton,
    Box,
    Flex,
    Icon,
    Heading,
    ModalCloseButton,
} from "@chakra-ui/react"
import formatDistanceToNow from "date-fns/formatDistanceToNow"
import React, { useState } from "react"
import ResponsiveTable, { KeyValues } from "../../../application/components/common/responsive_table/ResponsiveTable"
import { UserGameSummary } from "../types/UserGameSummary"
import { FaExternalLinkAlt, FaTrash } from "react-icons/fa"
import { useHistory } from "react-router-dom"
import Confirmation from "../../../application/components/common/Confirmation"
import { AppState } from "../../../application/store"
import { Dispatch } from "redux"
import { deleteGameAction, resetDeleteGameStatusAction } from "../store/Actions"
import { connect } from "react-redux"
import AlertBox from "../../../application/components/common/AlertBox"
import { OperationStatus } from "../../../application/store/async/AsyncOperationResult"
import { GiCardPick } from "react-icons/gi"
import { EMPTY_GAMES_LIST_PLACEHOLDER, LIST_GAMES_MODAL } from "./testids"

type Props = {
    isOpen: boolean
    onClose: () => void
    games: UserGameSummary[]
} & ReturnType<typeof mapStateToProps> &
    ReturnType<typeof mapDispatchToProps>

const optionalTimeDistance = (date?: string | null) =>
    date ? formatDistanceToNow(Date.parse(date), { addSuffix: true }) : "No"

const textWithTooltip = (content: string, tooltip?: string | null) => (
    <Tooltip label={tooltip} defaultIsOpen={false}>
        {content}
    </Tooltip>
)

const renderOwner = (game: UserGameSummary) =>
    game.isOwner ? (
        <Text casing="uppercase" fontWeight="bold" color="green">
            You
        </Text>
    ) : (
        game.ownerNickname
    )

const ContinueGameModal: React.FC<Props> = ({
    isOpen,
    onClose,
    games,
    deleteResult,
    deleteGame,
    resetDeleteResult,
}) => {
    const history = useHistory()
    const [gameToDelete, setGameToDelete] = useState<string | null>(null)

    const renderGoTo = (game: UserGameSummary) => (
        <Tooltip label="Go to game">
            <IconButton
                aria-label="Go to game"
                size="sm"
                icon={<FaExternalLinkAlt />}
                color="blue.500"
                onClick={() => history.push("/game/" + game.id)}
            />
        </Tooltip>
    )

    const renderDelete = (game: UserGameSummary) => (
        <IconButton
            aria-label="Delete game"
            size="sm"
            icon={<FaTrash />}
            color="red.500"
            onClick={() => setGameToDelete(game.id)}
            disabled={!game.isOwner}
        />
    )

    const data: KeyValues[] = [
        ["Navigate", games.map(renderGoTo)],
        ["Description", games.map((g) => g.description)],
        ["Game owner", games.map(renderOwner)],
        ["Your nickname", games.map((g) => g.playerNickname)],
        ["Role", games.map((g) => g.currentUserRole)],
        ["Started", games.map((g) => textWithTooltip(optionalTimeDistance(g.startedAt), g.startedAt))],
        ["Finished", games.map((g) => textWithTooltip(optionalTimeDistance(g.finishedAt), g.finishedAt))],
        ["Delete game", games.map(renderDelete)],
    ]

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            closeOnOverlayClick={true}
            closeOnEsc={true}
            size="full"
            autoFocus={false}
        >
            <ModalOverlay>
                <ModalContent>
                    <ModalBody data-testid={LIST_GAMES_MODAL}>
                        <Confirmation
                            visible={gameToDelete !== null}
                            onClose={() => setGameToDelete(null)}
                            header={"Delete game"}
                            content="Are you sure? Game will be permanently removed."
                            onConfirm={() => (gameToDelete ? deleteGame(gameToDelete) : void 0)}
                            size="sm"
                        />
                        {deleteResult.status === OperationStatus.FAILED && (
                            <AlertBox
                                title="Unable to delete game"
                                description={deleteResult.error?.message}
                                status="error"
                                onClose={() => resetDeleteResult()}
                            />
                        )}
                        {games.length > 0 ? <ResponsiveTable data={data} breakpointAt="lg" /> : <NoGamesPlaceholder />}
                        <ModalCloseButton />
                    </ModalBody>
                </ModalContent>
            </ModalOverlay>
        </Modal>
    )
}

const NoGamesPlaceholder: React.FC = () => (
    <Flex marginTop="10em" direction="column" alignItems="center" data-testid={EMPTY_GAMES_LIST_PLACEHOLDER}>
        <Box>
            <Icon as={GiCardPick} width="5em" height="5em" color="teal" />
        </Box>
        <Box>
            <Heading as="h3" size="lg" textAlign="center" color="teal">
                No previous games found
            </Heading>
        </Box>
    </Flex>
)

const mapStateToProps = (state: AppState) => ({
    deleteResult: state.games.deleteGameStatus,
})

const mapDispatchToProps = (dispatch: Dispatch) => ({
    deleteGame: (gameId: string) => dispatch(deleteGameAction.started(gameId)),
    resetDeleteResult: () => dispatch(resetDeleteGameStatusAction()),
})

export default connect(mapStateToProps, mapDispatchToProps)(ContinueGameModal)
