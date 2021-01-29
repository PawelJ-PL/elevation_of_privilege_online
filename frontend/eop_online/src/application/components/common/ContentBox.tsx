import { Box, BoxProps, Divider, Heading } from "@chakra-ui/react"
import React from "react"

type Props = {
    title?: string
    description?: string
    containerProps?: BoxProps & { "data-testid"?: string }
}

const ContentBox: React.FC<Props> = ({ children, title, description, containerProps }) => (
    <Box
        margin="0.5em"
        padding="1em"
        border="1px"
        borderColor="gray.400"
        backgroundColor="gray.100"
        borderRadius="0.3em"
        boxShadow="0.3em 0.3em 0.5em 0px rgba(0,0,0,0.3)"
        {...(containerProps || {})}
    >
        {title && <Heading size="lg">{title}</Heading>}
        {description && (
            <Heading size="md" opacity="0.4">
                {description}
            </Heading>
        )}
        {(title || description) && <Divider />}
        {children}
    </Box>
)

export default ContentBox
