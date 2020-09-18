import { Box, Icon, IconButton, Tooltip } from "@chakra-ui/core"
import React from "react"
import { AiOutlineUserDelete } from "react-icons/ai"
import { CgCardSpades } from "react-icons/cg"
import { FaHandPaper } from "react-icons/fa"
import { GiHouseKeys, GiSpyglass } from "react-icons/gi"
import { connect } from "react-redux"
import { Dispatch } from "redux"
import { assignUserRoleAction, kickUserAction } from "../../store/Actions"
import { Member, MemberRole } from "../../types/Member"

type Props = {
    gameId: string
    member: Member
    hideAddPlayer?: boolean
    hideAddObserver?: boolean
    hideKick?: boolean
    currentPlayerId: string
    ownerId: string
} & ReturnType<typeof mapDispatchToProps>

const RoleEntry: React.FC<Props> = ({
    gameId,
    member,
    hideAddPlayer,
    hideAddObserver,
    hideKick,
    currentPlayerId,
    ownerId,
    assignRole,
    kickUser,
}) => (
    <Box>
        {member.nickname}
        {ownerId === member.id && (
            <Tooltip label="Game owner">
                <span>
                    <Icon as={GiHouseKeys} boxSize={5} color="cyan.600" />
                </span>
            </Tooltip>
        )}
        {currentPlayerId === member.id && (
            <Tooltip label="You">
                <span>
                    <Icon as={FaHandPaper} boxSize={5} color="yellow.500" />
                </span>
            </Tooltip>
        )}{" "}
        {!hideAddPlayer && (
            <Tooltip label="Accept as a player">
                <IconButton
                    aria-label="Accept as a player"
                    icon={<CgCardSpades />}
                    size="sm"
                    colorScheme="teal"
                    variant="outline"
                    isRound={true}
                    marginRight="0.5em"
                    onClick={() => assignRole(gameId, member.id, "Player")}
                />
            </Tooltip>
        )}
        {!hideAddObserver && (
            <Tooltip label="Accept as an observer">
                <IconButton
                    aria-label="Accept as an observer"
                    icon={<GiSpyglass />}
                    size="sm"
                    colorScheme="teal"
                    variant="outline"
                    isRound={true}
                    marginRight="0.5em"
                    onClick={() => assignRole(gameId, member.id, "Observer")}
                />
            </Tooltip>
        )}
        {!hideKick && currentPlayerId !== member.id && (
            <Tooltip label="Kick">
                <IconButton
                    aria-label="Kick"
                    icon={<AiOutlineUserDelete />}
                    size="sm"
                    colorScheme="red"
                    variant="outline"
                    isRound={true}
                    marginRight="0.5em"
                    onClick={() => kickUser(gameId, member.id)}
                />
            </Tooltip>
        )}
    </Box>
)

const mapDispatchToProps = (dispatch: Dispatch) => ({
    assignRole: (gameId: string, participantId: string, role: MemberRole) =>
        dispatch(assignUserRoleAction.started({ gameId, participantId, role })),
    kickUser: (gameId: string, participantId: string) => dispatch(kickUserAction.started({ gameId, participantId })),
})

export default connect(null, mapDispatchToProps)(RoleEntry)
