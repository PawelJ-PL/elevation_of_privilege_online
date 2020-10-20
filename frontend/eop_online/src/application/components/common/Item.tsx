import { Box, BoxProps } from "@chakra-ui/core"
import React from "react"

type Props = {
    header: string
    content: string
    containerProps?: BoxProps
}

const Item: React.FC<Props> = ({ header, content, containerProps }) => (
    <Box {...(containerProps ?? {})}>
        <Box fontWeight="bold">{header}</Box>
        <Box color="rgba(0, 0, 0, 0.5)" lineHeight="0.7em">{content}</Box>
    </Box>
)

export default Item
