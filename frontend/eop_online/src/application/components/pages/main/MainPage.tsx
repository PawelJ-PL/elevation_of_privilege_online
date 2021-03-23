import { Box, Button, Flex, Icon, Image, Link, SimpleGrid } from "@chakra-ui/react"
import React, { useEffect } from "react"
import Logo from "../../../../resources/logo.png"
import { useModal } from "../../modals/ModalHook"
import CreateGameModal from "../../../../domain/game/components/CreateGameModal"
import GoToGameModal from "../../../../domain/game/components/GoToGameModal"
import { MAIN_PAGE_BUTTONS_CONTAINER } from "../testids"
import { AiFillGithub } from "react-icons/ai"
import ContinueGameModal from "../../../../domain/game/components/ContinueGameModal"
import { AppState } from "../../../store"
import { Dispatch } from "redux"
import { fetchUserGamesAction, resetUserGamesInfoAction } from "../../../../domain/game/store/Actions"
import { connect } from "react-redux"
import { OperationStatus } from "../../../store/async/AsyncOperationResult"
import AlertBox from "../../common/AlertBox"

type Props = ReturnType<typeof mapStateToProps> & ReturnType<typeof mapDispatchToProps>

const MainPage: React.FC<Props> = ({ fetchGames, fetchGamesStatus, resetGames }) => {
    useEffect(() => {
        if (fetchGamesStatus.status === OperationStatus.NOT_STARTED) {
            fetchGames()
        }
        return () => {
            resetGames()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    const [newGameModal, openCreateGameModal] = useModal(CreateGameModal, {})
    const [goToGameModal, openGoToGameModal] = useModal(GoToGameModal, {})
    const [continueGameModal, openContinueGameModal] = useModal(ContinueGameModal, {
        games: fetchGamesStatus.data ?? [],
    })

    return (
        <Box minHeight="100vh">
            {newGameModal}
            {goToGameModal}
            {continueGameModal}
            <Flex justify="center" backgroundColor="#23201f" width="100vw">
                <Image src={Logo} />
            </Flex>
            {fetchGamesStatus.status === OperationStatus.FAILED && (
                <AlertBox
                    status="error"
                    title="Unable to load previous games"
                    description={fetchGamesStatus.error?.message}
                    onClose={resetGames}
                />
            )}
            <SimpleGrid
                justifyContent={"center"}
                justifyItems={"center"}
                columns={[1, 1, 3, 3]}
                marginTop={["0.5em", "2em", "5em"]}
                marginX={["0em", "0em", "5em", "15em", "23em"]}
                data-testid={MAIN_PAGE_BUTTONS_CONTAINER}
            >
                <Button colorScheme="teal" size="lg" onClick={openCreateGameModal}>
                    Create new game
                </Button>
                <Button colorScheme="teal" size="lg" marginTop={["0.5em", "0.5em", "0em"]} onClick={openGoToGameModal}>
                    Join game
                </Button>
                <Button
                    colorScheme="teal"
                    size="lg"
                    marginTop={["0.5em", "0.5em", "0em"]}
                    onClick={openContinueGameModal}
                    disabled={fetchGamesStatus.status !== OperationStatus.FINISHED}
                    loadingText="Continue game"
                    isLoading={fetchGamesStatus.status === OperationStatus.PENDING}
                >
                    Continue game
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

const mapStateToProps = (state: AppState) => ({
    fetchGamesStatus: state.games.allGames,
})

const mapDispatchToProps = (dispatch: Dispatch) => ({
    fetchGames: () => dispatch(fetchUserGamesAction.started()),
    resetGames: () => dispatch(resetUserGamesInfoAction()),
})

export default connect(mapStateToProps, mapDispatchToProps)(MainPage)
