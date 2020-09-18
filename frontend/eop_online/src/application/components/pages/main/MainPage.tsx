import { Box, Button, Flex, Image, SimpleGrid } from "@chakra-ui/core"
import React from "react"
import Logo from "../../../../resources/logo.png"
import { useModal } from "../../modals/ModalHook"
import CreateGameModal from "../../../../domain/game/components/CreateGameModal"
import GoToGameModal from "../../../../domain/game/components/GoToGameModal"

const MainPage: React.FC = () => {
    const [newGameModal, openCreateGameModal] = useModal(CreateGameModal, {})
    const [goToGameModal, openGoToGameModal] = useModal(GoToGameModal, {})

    return (
        <Box>
            {newGameModal}
            {goToGameModal}
            <Flex justify="center" backgroundColor="#23201f" width="100vw">
                <Image src={Logo} />
            </Flex>
            <SimpleGrid
                justifyContent={"center"}
                justifyItems={"center"}
                columns={[1, 1, 2, 2]}
                marginTop={["0.5em", "2em", "5em"]}
                marginX={["0em", "0em", "5em", "20em"]}
            >
                <Button colorScheme="teal" size="lg" onClick={openCreateGameModal}>
                    Create new game
                </Button>
                <Button colorScheme="teal" size="lg" marginTop={["0.5em", "0.5em", "0em"]} onClick={openGoToGameModal}>
                    Join game
                </Button>
            </SimpleGrid>
        </Box>
    )
}

export default MainPage
