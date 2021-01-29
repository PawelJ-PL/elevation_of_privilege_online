import { Box, Center, Divider, Flex, Heading, Icon, Image, Tooltip } from "@chakra-ui/react"
import React, { useState } from "react"
import ContentBox from "../../../application/components/common/ContentBox"
import { loadCardImage } from "../../../application/utils/ResourceLoader"
import { Member } from "../../game/types/Member"
import { Card } from "../types/Card"
import { UsersCard } from "../types/UsersCard"
import CardZoomModal from "./CardZoomModal"
import { GiSandsOfTime } from "react-icons/gi"
import { HiThumbDown } from "react-icons/hi"
import { HiThumbUp } from "react-icons/hi"
import UnknownCard from "../../../resources/unknown-card.png"
import { TABLE_CARDS } from "./testids"

type Props = {
    matchId: string
    cards: UsersCard[]
    players: Member[]
}

const TableView: React.FC<Props> = ({ cards, players, matchId }) => {
    const [zoomedCard, setZoomedCard] = useState<Card | null>(null)

    const cardWidths = ["4rem", "6rem", "8rem", "8rem"]

    const threatIcon = (card: UsersCard) => {
        switch (card.threatLinked) {
            case true:
                return { icon: HiThumbUp, color: "green.400", comment: "Threat linked" }
            case false:
                return { icon: HiThumbDown, color: "red.400", comment: "Threat not linked" }
            default:
                return { icon: GiSandsOfTime, color: "gray.600", comment: "Waiting for decision" }
        }
    }

    const renderCard = (card: UsersCard) => (
        <Center key={card.card.cardNumber} margin="0.3rem" flexDirection="column" textAlign="center">
            <Heading as="h6" size="xs" maxWidth={cardWidths}>
                {players.find((p) => p.id === card.playerId)?.nickname}
            </Heading>
            <Box aria-label={threatIcon(card).comment}>
                <Tooltip label={threatIcon(card).comment}>
                    <span>
                        <Icon as={threatIcon(card).icon} color={threatIcon(card).color} boxSize={6} />
                    </span>
                </Tooltip>
            </Box>
            <Image
                cursor="pointer"
                key={card.card.cardNumber}
                src={loadCardImage(card.card)}
                maxWidth={cardWidths}
                onClick={() => setZoomedCard(card.card)}
                fallbackSrc={UnknownCard}
            />
        </Center>
    )

    return (
        <ContentBox containerProps={{ marginBottom: 0 }}>
            <CardZoomModal
                visible={zoomedCard !== null}
                onClose={() => setZoomedCard(null)}
                matchId={matchId}
                card={zoomedCard}
            />
            <Heading as="h4" size="md">
                Table
            </Heading>
            <Divider marginBottom="0.5em" />
            <Flex justifyContent="center" wrap="wrap" data-testid={TABLE_CARDS}>
                {cards.map(renderCard)}
            </Flex>
        </ContentBox>
    )
}

export default TableView
