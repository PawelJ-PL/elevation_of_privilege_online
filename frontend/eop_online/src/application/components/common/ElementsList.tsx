import { Box, BoxProps, Center, Divider, Heading } from "@chakra-ui/core"
import React from "react"

type Props = {
    elements: Array<JSX.Element>
    title?: string
    containerProps?: BoxProps
}
const ElementsList: React.FC<Props> = ({ elements, title, containerProps }) => (
    <Box border="2px" borderColor="gray.500" padding="0.2em" borderRadius="0.3em" {...(containerProps || {})}>
        {title && (
            <Center>
                <Heading size="sm" opacity="0.6">
                    {title}
                </Heading>
            </Center>
        )}
        {elements.map((elem, idx) => (
            <Box key={idx} marginTop="0.5em" paddingBottom="0.5em">
                {elem}
                {idx + 1 < elements.length && <Divider marginTop="0.5em" />}
            </Box>
        ))}
    </Box>
)

export default ElementsList
