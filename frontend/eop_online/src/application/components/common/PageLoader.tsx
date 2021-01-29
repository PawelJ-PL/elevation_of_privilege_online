import { Box, Flex, Spinner } from "@chakra-ui/react"
import React from "react"

const PageLoader: React.FC<{ text: string }> = ({ text }) => (
    <Flex justify="center" align="center" height="70vh" direction="column">
        <Spinner size="xl" thickness="0.3em" speed="0.65s" color="blue.600" />
        <Box fontWeight="bold">{text}</Box>
    </Flex>
)

export default PageLoader
