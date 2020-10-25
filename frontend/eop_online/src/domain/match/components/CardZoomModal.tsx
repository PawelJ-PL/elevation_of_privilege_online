import {
    Box,
    Button,
    Center,
    Flex,
    Heading,
    IconButton,
    Image,
    Modal,
    ModalBody,
    ModalCloseButton,
    ModalContent,
    ModalFooter,
    ModalOverlay,
    Tab,
    TabList,
    TabPanel,
    TabPanels,
    Tabs,
} from "@chakra-ui/core"
import React from "react"
import { HiThumbDown, HiThumbUp } from "react-icons/hi"
import { loadCardImage } from "../../../application/utils/ResourceLoader"
import { Card } from "../types/Card"
import UnknownCard from "../../../resources/unknown-card.png"

type Props = {
    visible: boolean
    onClose: () => void
    card: Card | null
    closable?: boolean
    canPlayCard?: boolean
    canLink?: boolean
}

const CardZoomModal: React.FC<Props> = ({ visible, onClose, card, closable, canPlayCard, canLink }) => {
    const playFooter = (card: Card) => (
        <Box width="100%">
            <Button width="100%" colorScheme="teal">
                Play this card
            </Button>
        </Box>
    )

    const linkFooter = (card: Card) => (
        <Flex justifyContent="center" alignItems="center" width="100%">
            <Heading as="h5" size="sm">
                Is the threat linked to the system?
            </Heading>
            <Box marginX="0.3em">
                <IconButton
                    icon={<HiThumbDown />}
                    variant="outline"
                    fontSize="1em"
                    colorScheme="red"
                    aria-label="Not linked"
                />
            </Box>
            <Box>
                <IconButton
                    icon={<HiThumbUp />}
                    variant="outline"
                    fontSize="1em"
                    colorScheme="green"
                    aria-label="Linked"
                />
            </Box>
        </Flex>
    )

    return (
        <Modal
            isOpen={visible}
            onClose={onClose}
            closeOnEsc={closable !== false}
            closeOnOverlayClick={closable !== false}
        >
            <ModalOverlay>
                {card && (
                    <ModalContent>
                        {closable !== false && <ModalCloseButton />}
                        <ModalBody>
                            <Center>
                                <Tabs variant="enclosed" isFitted={true}>
                                    <TabList>
                                        <Tab>Card</Tab>
                                        <Tab>Text</Tab>
                                    </TabList>
                                    <TabPanels>
                                        <TabPanel>
                                            <Image src={loadCardImage(card)} fallbackSrc={UnknownCard} />
                                        </TabPanel>
                                        <TabPanel>
                                            <Box width="270px">{card.text}</Box>
                                        </TabPanel>
                                    </TabPanels>
                                </Tabs>
                            </Center>
                        </ModalBody>
                        <ModalFooter>
                            {canPlayCard && playFooter(card)}
                            {canLink && linkFooter(card)}
                        </ModalFooter>
                    </ModalContent>
                )}
            </ModalOverlay>
        </Modal>
    )
}

export default CardZoomModal
