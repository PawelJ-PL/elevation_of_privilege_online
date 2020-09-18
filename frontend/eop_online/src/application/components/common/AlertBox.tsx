import { Alert, AlertDescription, AlertStatus, AlertTitle, BoxProps, CloseButton, Flex } from "@chakra-ui/core"
import React from "react"

type Props = {
    containerProps?: BoxProps
    title?: string
    description?: string
    onClose?: () => void
    status?: AlertStatus
    variant?: string
}

const AlertBox: React.FC<Props> = ({ containerProps, title, description, status, variant, onClose }) => {
    const baseAlertProps: BoxProps = { borderRadius: "0.5em" }
    const alertProps: BoxProps =
        containerProps === undefined ? baseAlertProps : { ...baseAlertProps, ...containerProps }

    return (
        <Alert {...alertProps} status={status} variant={variant}>
            <Flex direction="column">
                {title && <AlertTitle>{title}</AlertTitle>}
                {description && <AlertDescription fontSize="0.8em">{description}</AlertDescription>}
            </Flex>
            {onClose && <CloseButton marginLeft="auto" onClick={onClose} />}
        </Alert>
    )
}

export default AlertBox
