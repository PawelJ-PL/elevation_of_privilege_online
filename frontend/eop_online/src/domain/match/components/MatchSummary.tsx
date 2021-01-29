import { Box, Flex, Image } from "@chakra-ui/react"
import React from "react"
import ContentBox from "../../../application/components/common/ContentBox"
import { Game } from "../../game/types/Game"
import { RoundState } from "../types/RoundState"
import Item from "../../../application/components/common/Item"
import { loadSuitLogo } from "../../../application/utils/ResourceLoader"
import startCase from "lodash/startCase"
import capitalize from "lodash/capitalize"
import { LEADING_SUIT_BOX } from "./testids"

type Props = {
    game: Game
    roundState: RoundState
}

const MatchSummary: React.FC<Props> = ({ game, roundState }) => (
    <ContentBox title={game.description === null ? undefined : game.description}>
        <Flex marginTop="0.3em" align="center" data-testid={LEADING_SUIT_BOX}>
            {roundState.leadingSuit && (
                <Box>
                    <Image src={loadSuitLogo(roundState.leadingSuit)} />
                </Box>
            )}
            <Box>
                <Item header="Leading suit" content={capitalize(startCase(roundState.leadingSuit ?? "None"))} />
            </Box>
        </Flex>
    </ContentBox>
)

export default MatchSummary
