import {
    Accordion,
    AccordionButton,
    AccordionIcon,
    AccordionItem,
    AccordionPanel,
    Box,
    Divider,
    Image,
    Flex,
    Heading,
    Tab,
    TabList,
    TabPanel,
    TabPanels,
    Tabs,
    Text,
    useBreakpointValue,
} from "@chakra-ui/core"
import React, { useState } from "react"
import ContentBox from "../../../application/components/common/ContentBox"
import { Card } from "../types/Card"
import groupBy from "lodash/groupBy"
import startCase from "lodash/startCase"
import capitalize from "lodash/capitalize"
import sortBy from "lodash/sortBy"
import { loadCardImage } from "../../../application/utils/ResourceLoader"
import CardZoomModal from "./CardZoomModal"
import { Round } from "../types/Round"
import { Session } from "../../user/types/Session"
import UnknownCard from "../../../resources/unknown-card.png"

type Props = {
    round: Round
    currentUser: Session
}

const HandView: React.FC<Props> = ({ round, currentUser }) => {
    const [zoomedCard, setZoomedCard] = useState<Card | null>(null)

    const cardsBySuit = groupBy(round.hand, (c) => c.suit)

    const availableSuits: string[] = Object.keys(cardsBySuit)
        .filter((s) => s in cardsBySuit)
        .sort()
    const canPlay = (suit: string) =>
        currentUser.userId === round.state.currentPlayer &&
        (round.state.leadingSuit === suit ||
            !round.state.leadingSuit ||
            !availableSuits.includes(round.state.leadingSuit))

    const useTabMenu: boolean = useBreakpointValue({ base: false, md: true }) ?? true

    const suitHeader = (suit: string) => {
        return (
            <Box>
                <Text>{capitalize(startCase(suit))}</Text>
            </Box>
        )
    }

    const tabMenu = () => (
        <Tabs variant="enclosed">
            <TabList>
                {availableSuits.map((suit) => (
                    <Tab key={suit}>{suitHeader(suit)}</Tab>
                ))}
            </TabList>
            <TabPanels>
                {availableSuits.map((suit) => (
                    <TabPanel key={suit}>{suitPanel(suit)}</TabPanel>
                ))}
            </TabPanels>
        </Tabs>
    )

    const singleSuitAccordionItem = (suit: string) => (
        <AccordionItem key={suit}>
            <AccordionButton>
                <Box>{suitHeader(suit)}</Box>
                <AccordionIcon />
            </AccordionButton>
            <AccordionPanel>{suitPanel(suit)}</AccordionPanel>
        </AccordionItem>
    )

    const accordionMenu = () => (
        <Accordion defaultIndex={0} allowToggle={true}>
            {availableSuits.map(singleSuitAccordionItem)}
        </Accordion>
    )

    const suitPanel = (suitName: string) => {
        const sorted = sortBy(cardsBySuit[suitName], (c) => c.cardNumber)
        const cardWidths = ["4rem", "6rem", "8rem", "10rem"]
        return (
            <Flex wrap="wrap">
                {sorted.map((c) => (
                    <Box key={c.cardNumber} margin="0.3rem">
                        <Image
                            cursor="pointer"
                            src={loadCardImage(c)}
                            maxWidth={cardWidths}
                            onClick={() => setZoomedCard(c)}
                            fallbackSrc={UnknownCard}
                        />
                    </Box>
                ))}
            </Flex>
        )
    }

    return (
        <ContentBox containerProps={{ marginBottom: 0 }}>
            <CardZoomModal
                visible={zoomedCard !== null}
                onClose={() => setZoomedCard(null)}
                card={zoomedCard}
                canPlayCard={zoomedCard ? canPlay(zoomedCard.suit) : false}
            />
            <Heading as="h4" size="md">
                Hand
            </Heading>
            <Divider marginBottom="0.5em" />
            {useTabMenu ? tabMenu() : accordionMenu()}
        </ContentBox>
    )
}

export default HandView
