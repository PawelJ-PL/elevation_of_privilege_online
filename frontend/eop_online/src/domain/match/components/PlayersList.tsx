import { Box, BoxProps, Divider, Flex, Heading } from "@chakra-ui/core"
import React from "react"
import ContentBox from "../../../application/components/common/ContentBox"
import { Member } from "../../game/types/Member"
import { Session } from "../../user/types/Session"
import { RoundState } from "../types/RoundState"

type Props = {
    players: Member[]
    roundState: RoundState
    currentUser: Session
    scores: Record<string, number>
}

const PlayersList: React.FC<Props> = ({ players, roundState, currentUser, scores }) => {
    const playerDisplayProps = (player: Member) => {
        let props: BoxProps = {}
        if (player.id === roundState.currentPlayer) {
            props = { backgroundColor: "rgba(79, 209, 197,0.5)" }
        }
        if (player.id === currentUser.userId) {
            props = { ...props, fontWeight: "bold" }
        }
        return props
    }

    return (
        <ContentBox containerProps={{ marginBottom: 0, paddingX: "0.1em" }}>
            <Box marginBottom="0.5em" paddingX="0.5em">
                <Heading as="h4" size="md">
                    Players
                </Heading>
                <Divider />
            </Box>

            {players.map((p) => (
                <Flex
                    justifyContent="space-between"
                    borderRadius="0.2em"
                    paddingX="0.5em"
                    key={p.id}
                    {...playerDisplayProps(p)}
                >
                    <Box>{p.nickname}</Box>
                    <Box>{scores[p.id] ?? 0}</Box>
                </Flex>
            ))}
        </ContentBox>
    )
}

export default PlayersList
