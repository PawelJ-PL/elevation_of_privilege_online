import { Modal, ModalBody, ModalContent, ModalOverlay } from "@chakra-ui/react"
import React from "react"
import NewGameForm from "./NewGameForm"

type Props = { isOpen: boolean; onClose: () => void }

const CreateGameModal: React.FC<Props> = ({ isOpen, onClose }) => {
    return (
        <Modal isOpen={isOpen} onClose={onClose} closeOnOverlayClick={false} closeOnEsc={false}>
            <ModalOverlay>
                <ModalContent>
                    <ModalBody>
                        <NewGameForm onCancel={onClose} />
                    </ModalBody>
                </ModalContent>
            </ModalOverlay>
        </Modal>
    )
}

export default CreateGameModal
