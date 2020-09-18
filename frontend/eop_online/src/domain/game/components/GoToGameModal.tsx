import { Button, Modal, ModalBody, ModalContent, ModalFooter, ModalOverlay } from "@chakra-ui/core"
import React, { useState } from "react"
import { useHistory } from "react-router-dom"
import FormInput from "../../../application/components/common/FormInput"

type Props = {
    isOpen: boolean
    onClose: () => void
}

const GoToGameModal: React.FC<Props> = ({ isOpen, onClose }) => {
    const [gameId, setGameId] = useState("")
    const [gameIdTouched, setGameIdTouched] = useState(false)

    const canSubmit = gameId.trim().length > 0
    const errorMessage = gameIdTouched && !canSubmit ? "Game ID is required" : undefined

    const history = useHistory()

    return (
        <Modal isOpen={isOpen} onClose={onClose}>
            <ModalOverlay>
                <ModalContent>
                    <ModalBody>
                        <FormInput
                            inputId="gameIdField"
                            required={true}
                            errorMessage={errorMessage}
                            label="Game ID"
                            value={gameId}
                            onTextChange={(data) => {
                                setGameId(data)
                                setGameIdTouched(true)
                            }}
                        />
                    </ModalBody>
                    <ModalFooter>
                        <Button size="sm" marginRight="0.3em" colorScheme="gray" onClick={onClose}>
                            Back
                        </Button>
                        <Button
                            size="sm"
                            marginLeft="0.3em"
                            colorScheme="blue"
                            onClick={() => history.push("/game/" + gameId)}
                            isDisabled={!canSubmit}
                        >
                            Continue
                        </Button>
                    </ModalFooter>
                </ModalContent>
            </ModalOverlay>
        </Modal>
    )
}

export default GoToGameModal
