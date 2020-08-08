package com.github.paweljpl.eoponline

import java.time.Instant

import com.github.pawelj_pl.eoponline.game.{Game, Player, PlayerRole}
import io.chrisdavenport.fuuid.FUUID

trait Constants {

  final val ExampleId1 = FUUID.fuuid("8233feda-c43a-45e8-9faf-f80040b29fc7")

  final val ExampleId2 = FUUID.fuuid("05a7bc08-7ecc-4f8f-aaa6-9504717c4aa5")

  final val FirstRandomFuuid = FUUID.fuuid("82d50077-c7fa-48c8-ad4f-362cdffa4b0c")

  final val ExampleNickName = "Roman"

  final val ExampleUserId = FUUID.fuuid("bae73e07-20e8-4829-bef2-1d6f91db8f4e")

  final val ExampleGameId = FUUID.fuuid("da2cbc28-dca8-430f-a61d-0714f954ac02")

  final val ExampleGameDescription = "Some description"

  final val ExampleGameStartTime = Instant.ofEpochMilli(1577912400000L)

  final val ExampleGame = Game(ExampleGameId, Some(ExampleGameDescription), ExampleUserId, Some(ExampleGameStartTime))

  final val ExampleNotStartedGame = ExampleGame.copy(startedAt = None)

  final val ExamplePlayer = Player(ExampleUserId, ExampleNickName, Some(PlayerRole.Player))

}
