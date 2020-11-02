import { Box, Flex } from "@chakra-ui/core"
import React, { useEffect, useState } from "react"
import { Game } from "../../game/types/Game"
import { Member } from "../../game/types/Member"
import { Session } from "../../user/types/Session"
import { Card } from "../types/Card"
import { Round } from "../types/Round"
import CardZoomModal from "./CardZoomModal"
import HandView from "./HandView"
import MatchSummary from "./MatchSummary"
import PlayersList from "./PlayersList"
import TableView from "./TableView"

type Props = {
    game: Game
    round: Round
    user: Session
    members: Member[]
    scores: Record<string, number>
}

const MatchView: React.FC<Props> = ({ game, round, members, user, scores }) => {
    const [zoomedCard, setZoomedCard] = useState<Card | null>(null)
    useEffect(() => {
        const waitingCard = round.table.find(
            (c) => c.playerId === user.userId && (c.threatLinked === null || c.threatLinked === undefined)
        )
        setZoomedCard(waitingCard?.card ?? null)
    }, [round, user])

    const players = members.filter((p) => p.role === "Player")

    return (
        <Flex direction="column">
            <Box order={1}>
                <MatchSummary game={game} roundState={round.state} />
            </Box>

            <Flex
                direction={["column", "column", "column", "row"]}
                alignItems={["stretch", "stretch", "stretch", "center"]}
                order={[3, 3, 3, 2]}
            >
                <Box flexGrow={[0, 0, 0, 5]}>
                    <TableView cards={round.table} players={players} matchId={game.id} />
                </Box>
                <Box flexGrow={[0, 0, 0, 1]}>
                    <PlayersList players={players} roundState={round.state} currentUser={user} scores={scores} />
                </Box>
            </Flex>

            {players.map((p) => p.id).includes(user.userId) && (
                <Box order={[2, 2, 2, 3]}>
                    {zoomedCard && (
                        <CardZoomModal
                            visible={true}
                            onClose={() => setZoomedCard(null)}
                            matchId={round.state.gameId}
                            card={zoomedCard}
                            closable={false}
                            canLink={true}
                        />
                    )}
                    <HandView round={round} currentUser={user} />
                </Box>
            )}
        </Flex>
    )
}

export default MatchView
