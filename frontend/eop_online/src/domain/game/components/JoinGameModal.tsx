import {
    Button,
    Modal,
    ModalBody,
    ModalContent,
    ModalFooter,
    ModalHeader,
    ModalOverlay,
    useToast,
} from "@chakra-ui/react"
import React, { useEffect, useState } from "react"
import { connect } from "react-redux"
import { useHistory } from "react-router-dom"
import { Dispatch } from "redux"
import FormInput from "../../../application/components/common/FormInput"
import { AppState } from "../../../application/store"
import { OperationStatus } from "../../../application/store/async/AsyncOperationResult"
import { fetchGameInfoAction, joinGameAction, resetJoinStatusAction } from "../store/Actions"
import { GameAlreadyStarted, GameNotFound, UserAlreadyJoined } from "../types/Errors"

type Props = {
    isOpen: boolean
    onClose: () => void
    gameId: string
} & ReturnType<typeof mapStateToProps> &
    ReturnType<typeof mapDispatchToProps>

const JoinGameModal: React.FC<Props> = ({
    isOpen,
    onClose,
    gameId,
    joinGame,
    resetJoinStatus,
    joinStatus,
    fetchGameInfo,
}) => {
    const history = useHistory()
    const [nickname, setNickname] = useState("")
    const [nicknameTouched, setNicknameTouched] = useState(false)

    const canSubmit = nickname.trim().length > 0
    const fieldError = nicknameTouched && !canSubmit ? "Nickname is required" : undefined

    const toast = useToast()

    useEffect(() => {
        return () => {
            resetJoinStatus()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    useEffect(() => {
        if (joinStatus.status === OperationStatus.FINISHED || joinStatus.error instanceof UserAlreadyJoined) {
            fetchGameInfo(gameId)
        } else if (joinStatus.error instanceof GameNotFound || joinStatus.error instanceof GameAlreadyStarted) {
            toast({ title: "Game not found", position: "top", duration: 3000, status: "warning" })
            history.push("/")
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [joinStatus, gameId])

    return (
        <Modal isOpen={isOpen} onClose={onClose}>
            <ModalOverlay>
                <ModalContent>
                    <ModalHeader>Join the game</ModalHeader>
                    <ModalBody>
                        <FormInput
                            inputId="nicknameField"
                            label="Your nickname"
                            required={true}
                            errorMessage={fieldError}
                            value={nickname}
                            onTextChange={(data) => {
                                setNickname(data)
                                setNicknameTouched(true)
                            }}
                        />
                    </ModalBody>
                    <ModalFooter>
                        <Button
                            size="xs"
                            marginRight="0.3em"
                            colorScheme="gray"
                            onClick={() => history.push("/")}
                            isLoading={joinStatus.status === OperationStatus.PENDING}
                            loadingText="Back"
                        >
                            Back
                        </Button>
                        <Button
                            size="xs"
                            marginLeft="0.3em"
                            colorScheme="blue"
                            isDisabled={!canSubmit}
                            onClick={() => joinGame(gameId, nickname)}
                            isLoading={joinStatus.status === OperationStatus.PENDING}
                            loadingText="Join"
                        >
                            Join
                        </Button>
                    </ModalFooter>
                </ModalContent>
            </ModalOverlay>
        </Modal>
    )
}

const mapStateToProps = (state: AppState) => ({
    joinStatus: state.games.joinGame,
})

const mapDispatchToProps = (dispatch: Dispatch) => ({
    joinGame: (gameId: string, nickname: string) => dispatch(joinGameAction.started({ gameId, nickname })),
    resetJoinStatus: () => dispatch(resetJoinStatusAction()),
    fetchGameInfo: (gameId: string) => dispatch(fetchGameInfoAction.started(gameId)),
})

export default connect(mapStateToProps, mapDispatchToProps)(JoinGameModal)
