import { Center, Flex, Icon, Skeleton, Stat, StatHelpText, StatLabel, StatNumber, Text } from "@chakra-ui/core"
import format from "date-fns/format"
import formatDistance from "date-fns/formatDistance"
import formatDistanceToNow from "date-fns/formatDistanceToNow"
import parseJSON from "date-fns/parseJSON"
import React, { useEffect } from "react"
import { connect } from "react-redux"
import { Dispatch } from "redux"
import AlertBox from "../../../application/components/common/AlertBox"
import ContentBox from "../../../application/components/common/ContentBox"
import ElementsList from "../../../application/components/common/ElementsList"
import { AppState } from "../../../application/store"
import { fetchScoresAction } from "../../match/store/Actions"
import { fetchMembersAction } from "../store/Actions"
import { Game } from "../types/Game"
import { Member } from "../types/Member"
import sortBy from "lodash/sortBy"
import { FaTrophy } from "react-icons/fa"
import { PLAYER_RANKING_ENTRY } from "./testids"

type Props = {
    game: Game
} & ReturnType<typeof mapStateToProps> &
    ReturnType<typeof mapDispatchToProps>

const renderTimeStats = (label: string, time: Date) => (
    <Stat>
        <StatLabel>{label}</StatLabel>
        <StatNumber>{formatDistanceToNow(time, { addSuffix: true })}</StatNumber>
        <StatHelpText>{format(time, "do MMM y HH:mm")}</StatHelpText>
    </Stat>
)

const positionToColor: Record<number, string> = {
    1: "yellow.500",
    2: "gray.400",
    3: "yellow.800",
}

const renderPlayerEntry = (player: Member & { scores: number }, position: number) => (
    <Flex alignItems="center" justifyContent="center" data-testid={PLAYER_RANKING_ENTRY}>
        <Text fontWeight="bold">{position}) </Text>
        <Text marginX="0.3em">
            {player.nickname} ({player.scores} points)
        </Text>
        {positionToColor[position] && <Icon as={FaTrophy} boxSize="6" color={positionToColor[position]} />}
    </Flex>
)

export const AfterGameSummary: React.FC<Props> = ({ game, members, scores, fetchMembers, fetchScores }) => {
    useEffect(() => {
        fetchScores(game.id)
        fetchMembers(game.id)
    }, [fetchMembers, fetchScores, game])

    const startedAt = game.startedAt ? parseJSON(game.startedAt) : undefined
    const finishedAt = game.finishedAt ? parseJSON(game.finishedAt) : undefined

    const playerElements = () => {
        if (scores.error || members.error) {
            return [
                <AlertBox
                    key="1"
                    title="Unable to fetch winners data"
                    description={scores.error?.message ?? members.error?.message}
                    status="error"
                    variant="solid"
                />,
            ]
        } else if (members.data && scores.data) {
            const playersOnly = members.data
                .filter((m) => m.role === "Player")
                .map((p) => ({ ...p, scores: (scores.data || {})[p.id] ?? 0 }))
            return sortBy(playersOnly, (p) => p.scores)
                .reverse()
                .map((player, idx) => renderPlayerEntry(player, idx + 1))
        } else {
            return [<Skeleton key="1" height="8em" />]
        }
    }

    return (
        <ContentBox title="Game finished" description={game.description ?? undefined}>
            {startedAt && finishedAt && (
                <Flex
                    marginTop="0.5em"
                    flexDirection={["column", "column", "row"]}
                    justifyContent={["flex-start", "flex-start", "space-around"]}
                >
                    {renderTimeStats("Game started", startedAt)}
                    {renderTimeStats("Game finished", finishedAt)}
                    <Stat>
                        <StatLabel>Game Duration</StatLabel>
                        <StatNumber>{formatDistance(finishedAt, startedAt)}</StatNumber>
                    </Stat>
                </Flex>
            )}
            <Center>
                <ElementsList
                    containerProps={{ minWidth: "30%", minHeight: "10em", marginTop: "2em" }}
                    title="Players"
                    elements={playerElements()}
                />
            </Center>
        </ContentBox>
    )
}

const mapStateToProps = (state: AppState) => ({
    members: state.games.members,
    scores: state.matches.fetchScores,
})

const mapDispatchToProps = (dispatch: Dispatch) => ({
    fetchMembers: (gameId: string) => dispatch(fetchMembersAction.started(gameId)),
    fetchScores: (gameId: string) => dispatch(fetchScoresAction.started(gameId)),
})

export default connect(mapStateToProps, mapDispatchToProps)(AfterGameSummary)
