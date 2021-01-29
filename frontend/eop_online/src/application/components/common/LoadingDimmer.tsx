import { Box } from "@chakra-ui/react"
import React from "react"
import PageLoader from "./PageLoader"

type Props = {
    active?: boolean
    text?: string
}

const LoadingDimmer: React.FC<Props> = ({ active, text }) => {
    return (
        <Box
            position="fixed"
            top={0}
            left={0}
            bottom={0}
            right={0}
            display={active ? "block" : "none"}
            backgroundColor="rgba(0, 0, 0, 0.4)"
            zIndex={9999}
            style={{ backdropFilter: "blur(4px)" }}
        >
            <PageLoader text={text ?? "Loading"} />
        </Box>
    )
}

export default LoadingDimmer
