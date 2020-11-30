import { Box, Button, Flex, Icon, Image, Link, SimpleGrid } from "@chakra-ui/core"
import React from "react"
import Logo from "../../../../resources/logo.png"
import { useModal } from "../../modals/ModalHook"
import CreateGameModal from "../../../../domain/game/components/CreateGameModal"
import GoToGameModal from "../../../../domain/game/components/GoToGameModal"
import { MAIN_PAGE_BUTTONS_CONTAINER } from "../testids"
import { AiFillGithub } from "react-icons/ai"

const MainPage: React.FC = () => {
    const [newGameModal, openCreateGameModal] = useModal(CreateGameModal, {})
    const [goToGameModal, openGoToGameModal] = useModal(GoToGameModal, {})

    return (
        <Box minHeight="100vh">
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
                data-testid={MAIN_PAGE_BUTTONS_CONTAINER}
            >
                <Button colorScheme="teal" size="lg" onClick={openCreateGameModal}>
                    Create new game
                </Button>
                <Button colorScheme="teal" size="lg" marginTop={["0.5em", "0.5em", "0em"]} onClick={openGoToGameModal}>
                    Join game
                </Button>
            </SimpleGrid>
            <Box textAlign="center" position="fixed" bottom="1em" width="100%">
                <Link href="https://github.com/PawelJ-PL/elevation_of_privilege_online" isExternal={true}>
                    <Icon as={AiFillGithub} /> Visit on Github
                </Link>
            </Box>
        </Box>
    )
}

export default MainPage
