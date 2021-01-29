import {
    Accordion,
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
    useBreakpointValue,
} from "@chakra-ui/react"
import React, { useEffect } from "react"
import { HiThumbDown, HiThumbUp } from "react-icons/hi"
import { loadCardImage } from "../../../application/utils/ResourceLoader"
import { Card } from "../types/Card"
import UnknownCard from "../../../resources/unknown-card.png"
import { AppState } from "../../../application/store"
import { Dispatch } from "redux"
import {
    putCardOnTableAction,
    resetPutCardOnTableStatusAction,
    resetUpdateCardOnTablesStatusAction,
    updateCardOnTableAction,
} from "../store/Actions"
import { connect } from "react-redux"
import { OperationStatus } from "../../../application/store/async/AsyncOperationResult"
import AlertBox from "../../../application/components/common/AlertBox"
import { ZOOM_CARD_MODAL_FOOTER } from "./testids"
import AccordionEntry from "../../../application/components/common/AccordionEntry"

const multilineTextToTags = (text: string) => text.split("\n").map((line, i) => <Box key={i}>{line}</Box>)

type MenuProps = { card: Card }

const TabMenu: React.FC<MenuProps> = ({ card }) => (
    <Tabs variant="enclosed" isFitted={true}>
        <TabList>
            <Tab>Card</Tab>
            <Tab>Text</Tab>
            <Tab>Example</Tab>
            <Tab>Mitigation</Tab>
        </TabList>
        <TabPanels>
            <TabPanel>
                <Image src={loadCardImage(card)} fallbackSrc={UnknownCard} />
            </TabPanel>
            <TabPanel>
                <Box width="270px">{card.text}</Box>
            </TabPanel>
            <TabPanel>
                <Box width="270px">{multilineTextToTags(card.example)}</Box>
            </TabPanel>
            <TabPanel>
                <Box width="270px">{multilineTextToTags(card.mitigation)}</Box>
            </TabPanel>
        </TabPanels>
    </Tabs>
)

const AccordionMenu: React.FC<MenuProps> = ({ card }) => (
    <Accordion allowToggle={true} defaultIndex={0} width="270px">
        <AccordionEntry title="Card" content={<Image src={loadCardImage(card)} fallbackSrc={UnknownCard} />} />
        <AccordionEntry title="Text" content={card.text} />
        <AccordionEntry title="Example" content={<Box>{multilineTextToTags(card.example)}</Box>} />
        <AccordionEntry title="Mitigation" content={<Box>{multilineTextToTags(card.mitigation)}</Box>} />
    </Accordion>
)

type Props = {
    visible: boolean
    onClose: () => void
    matchId: string
    card: Card | null
    closable?: boolean
    canPlayCard?: boolean
    canLink?: boolean
} & ReturnType<typeof mapDispatchToProps> &
    ReturnType<typeof mapStateToProps>

const CardZoomModal: React.FC<Props> = ({
    visible,
    onClose,
    matchId,
    card,
    closable,
    canPlayCard,
    canLink,
    setThreatLinkedStatus,
    updateCardStatus,
    resetLinkedStatus,
    playCard,
    resetPlayStatus,
    playCardStatus,
}) => {
    useEffect(() => {
        if (updateCardStatus.status === OperationStatus.FINISHED) {
            resetLinkedStatus()
            onClose()
        }
    }, [updateCardStatus, resetLinkedStatus, onClose])

    const useTabMenu: boolean = useBreakpointValue({ base: false, sm: true }) ?? true

    const playFooter = (card: Card) => (
        <Box width="100%">
            <Button
                width="100%"
                colorScheme="teal"
                onClick={() => playCard(matchId, card.cardNumber)}
                isLoading={playCardStatus.status === OperationStatus.PENDING}
            >
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
                    onClick={() => setThreatLinkedStatus(matchId, card.cardNumber, false)}
                    isLoading={updateCardStatus.status === OperationStatus.PENDING}
                />
            </Box>
            <Box>
                <IconButton
                    icon={<HiThumbUp />}
                    variant="outline"
                    fontSize="1em"
                    colorScheme="green"
                    aria-label="Linked"
                    onClick={() => setThreatLinkedStatus(matchId, card.cardNumber, true)}
                    isLoading={updateCardStatus.status === OperationStatus.PENDING}
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
                            <Center>{useTabMenu ? <TabMenu card={card} /> : <AccordionMenu card={card} />}</Center>
                            {updateCardStatus.status === OperationStatus.FAILED && (
                                <AlertBox
                                    title="Unable to change linked status"
                                    description={updateCardStatus.error?.message}
                                    status="error"
                                    onClose={resetLinkedStatus}
                                    variant="solid"
                                    containerProps={{ marginTop: "0.5em" }}
                                />
                            )}
                            {playCardStatus.status === OperationStatus.FAILED && (
                                <AlertBox
                                    title="Unable to play the card"
                                    description={playCardStatus.error?.message}
                                    status="error"
                                    onClose={resetPlayStatus}
                                    variant="solid"
                                    containerProps={{ marginTop: "0.5em" }}
                                />
                            )}
                        </ModalBody>
                        <ModalFooter data-testid={ZOOM_CARD_MODAL_FOOTER}>
                            {canPlayCard && playFooter(card)}
                            {canLink && linkFooter(card)}
                        </ModalFooter>
                    </ModalContent>
                )}
            </ModalOverlay>
        </Modal>
    )
}

const mapStateToProps = (state: AppState) => ({
    updateCardStatus: state.matches.updateCardOnTable,
    playCardStatus: state.matches.putCardOnTable,
})

const mapDispatchToProps = (dispatch: Dispatch) => ({
    setThreatLinkedStatus: (matchId: string, cardNumber: number, threatLinked: boolean) =>
        dispatch(updateCardOnTableAction.started({ matchId, cardNumber, data: { threatLinked } })),
    resetLinkedStatus: () => dispatch(resetUpdateCardOnTablesStatusAction()),
    playCard: (matchId: string, cardNumber: number) => dispatch(putCardOnTableAction.started({ matchId, cardNumber })),
    resetPlayStatus: () => dispatch(resetPutCardOnTableStatusAction()),
})

export default connect(mapStateToProps, mapDispatchToProps)(CardZoomModal)
