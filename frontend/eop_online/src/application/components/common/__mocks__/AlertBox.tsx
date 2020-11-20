import React from "react"
import { AlertStatus } from "@chakra-ui/core"

type Props = {
    title?: string
    description?: string
    status?: AlertStatus
}

const AlertBox: React.FC<Props> = ({ title, description, status }) => (
    <div>
        <div data-testid="ALERT_MOCK">
            <div>{title ?? ""}</div>
            <div>{description ?? ""}</div>
            <div>{status}</div>
        </div>
    </div>
)

export default AlertBox
