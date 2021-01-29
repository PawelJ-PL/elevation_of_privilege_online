import { Box, Button, Flex, FormControl, FormLabel, Textarea, useToast } from "@chakra-ui/react"
import React, { useEffect, useState } from "react"
import { connect } from "react-redux"
import { Dispatch } from "redux"
import AlertBox from "../../../application/components/common/AlertBox"
import FormInput from "../../../application/components/common/FormInput"
import { AppState } from "../../../application/store"
import { OperationStatus } from "../../../application/store/async/AsyncOperationResult"
import { createGameAction, resetCreateGameStatusAction } from "../store/Actions"

type Props = {
    onCancel: () => void
} & ReturnType<typeof mapStateToProps> &
    ReturnType<typeof mapDispatchToProps>

const NewGameForm: React.FC<Props> = ({ onCancel, createGameStatus, createGame, resetStatus }) => {
    useEffect(() => {
        return () => {
            resetStatus()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    const toast = useToast()
    useEffect(() => {
        if (createGameStatus.status === OperationStatus.FINISHED) {
            toast({ title: "Game created", status: "success", duration: 2000, position: "top" })
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [createGameStatus])

    const [nickName, setNickname] = useState("")
    const [nickNameTouched, setNicknameTouched] = useState(false)
    const [description, setDescription] = useState("")

    const handleSubmit = () => {
        const possibleEmptyDescription = description.trim().length > 0 ? description : undefined
        createGame(nickName, possibleEmptyDescription)
    }

    const canSubmit = nickName.trim().length > 0

    const inputErrorMessage = nickNameTouched && !canSubmit ? "Nickname is required" : undefined

    return (
        <Box>
            {createGameStatus.status === OperationStatus.FAILED && (
                <AlertBox
                    status="error"
                    title="Unable to create game"
                    description={createGameStatus.error?.message}
                    onClose={resetStatus}
                />
            )}
            <FormInput
                required={true}
                errorMessage={inputErrorMessage}
                inputId="nickname"
                label="Your nickname"
                value={nickName}
                onTextChange={(nick) => {
                    setNickname(nick)
                    setNicknameTouched(true)
                }}
            />
            <FormControl>
                <FormLabel htmlFor="description">Game description</FormLabel>
                <Textarea
                    id="description"
                    size="sm"
                    value={description}
                    onChange={(event) => setDescription(event.target.value)}
                />
            </FormControl>
            <Flex marginTop="0.5em" justifyContent="flex-end">
                <Button
                    size="xs"
                    onClick={onCancel}
                    colorScheme="gray"
                    marginX={"0.2em"}
                    isDisabled={createGameStatus.status === OperationStatus.PENDING}
                >
                    Cancel
                </Button>
                <Button
                    size="xs"
                    colorScheme="blue"
                    marginX={"0.2em"}
                    isDisabled={!canSubmit}
                    onClick={handleSubmit}
                    isLoading={createGameStatus.status === OperationStatus.PENDING}
                    loadingText="Create"
                >
                    Create
                </Button>
            </Flex>
        </Box>
    )
}

const mapStateToProps = (state: AppState) => ({
    createGameStatus: state.games.createStatus,
})

const mapDispatchToProps = (dispatch: Dispatch) => ({
    createGame: (ownerNickname: string, description?: string) =>
        dispatch(createGameAction.started({ ownerNickname, description })),
    resetStatus: () => dispatch(resetCreateGameStatusAction()),
})

export default connect(mapStateToProps, mapDispatchToProps)(NewGameForm)
