import { AccordionButton, AccordionIcon, AccordionItem, AccordionPanel, Box, Heading } from "@chakra-ui/core"
import React from "react"

type Props = { title: string; content: JSX.Element | string }

const AccordionEntry: React.FC<Props> = ({ title, content }) => (
    <AccordionItem key={title}>
        <AccordionButton>
            <Box>
                <Heading as="h5" size="sm">
                    {title}
                </Heading>
            </Box>
            <AccordionIcon />
        </AccordionButton>
        <AccordionPanel>{content}</AccordionPanel>
    </AccordionItem>
)

export default AccordionEntry
