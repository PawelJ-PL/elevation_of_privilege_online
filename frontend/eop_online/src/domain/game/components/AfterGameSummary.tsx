import { Box } from "@chakra-ui/core"
import React from "react"
import { Game } from "../types/Game"

type Props = {
    game: Game
}

const AfterGameSummary: React.FC<Props> = ({ game }) => {
    return <Box>{JSON.stringify(game)}</Box>
}

export default AfterGameSummary
