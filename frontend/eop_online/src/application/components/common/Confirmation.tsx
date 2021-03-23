import { Button } from "@chakra-ui/button"
import {
    AlertDialog,
    AlertDialogBody,
    AlertDialogContent,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogOverlay,
} from "@chakra-ui/modal"
import React from "react"

type Props = {
    visible: boolean
    onClose: () => void
    header?: string
    content: string
    onConfirm: () => void
    size?: "sm" | "md" | "lg" | "xl" | "2xl" | "xs" | "3xl" | "4xl" | "5xl" | "6xl" | "full"
}

const Confirmation: React.FC<Props> = ({ visible, onClose, header, content, onConfirm, size }) => {
    const cancelRef = React.useRef<HTMLButtonElement>(null)

    return (
        <AlertDialog isOpen={visible} onClose={onClose} leastDestructiveRef={cancelRef} size={size}>
            <AlertDialogOverlay>
                <AlertDialogContent>
                    {header && <AlertDialogHeader fontWeight="bold">{header}</AlertDialogHeader>}

                    <AlertDialogBody>{content}</AlertDialogBody>

                    <AlertDialogFooter>
                        <Button ref={cancelRef} onClick={onClose}>
                            Cancel
                        </Button>
                        <Button
                            colorScheme="red"
                            marginLeft="0.3em"
                            onClick={() => {
                                onConfirm()
                                onClose()
                            }}
                        >
                            Confirm
                        </Button>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialogOverlay>
        </AlertDialog>
    )
}

export default Confirmation
