package com.github.paweljpl.eoponline

import java.time.Instant

import com.github.pawelj_pl.eoponline.game.{Game, Player, PlayerRole}
import io.chrisdavenport.fuuid.FUUID

trait Constants {

  final val ExampleId1 = FUUID.fuuid("8233feda-c43a-45e8-9faf-f80040b29fc7")

  final val ExampleId2 = FUUID.fuuid("05a7bc08-7ecc-4f8f-aaa6-9504717c4aa5")

  final val ExampleId3 = FUUID.fuuid("5030eea3-6f18-4f85-9919-2cf08df4f54b")

  final val ExampleId4 = FUUID.fuuid("30f57e28-fae1-4bb9-b20d-a05fa5c2ec54")

  final val ExampleId5 = FUUID.fuuid("9c7a34ce-d751-468b-8d1d-752d2990bb2b")

  final val ExampleId6 = FUUID.fuuid("9d627e35-3a60-40e8-a440-cce15392b9d1")

  final val ExampleId7 = FUUID.fuuid("d291df42-dc36-41fb-a7f4-0cc02dd2b019")

  final val ExampleId8 = FUUID.fuuid("f8e9d8d1-be98-481f-b54a-5a98f6fc965c")

  final val ExampleId9 = FUUID.fuuid("fb16444b-6f07-4bef-84b8-00f2b5e91c76")

  final val ExampleId10 = FUUID.fuuid("33ab752c-12bb-4700-b8f3-b3d7bb5cc6f5")

  final val FirstRandomFuuid = FUUID.fuuid("82d50077-c7fa-48c8-ad4f-362cdffa4b0c")

  final val ExampleNickName = "Roman"

  final val ExampleUserId = FUUID.fuuid("bae73e07-20e8-4829-bef2-1d6f91db8f4e")

  final val ExampleGameId = FUUID.fuuid("da2cbc28-dca8-430f-a61d-0714f954ac02")

  final val ExampleGameDescription = "Some description"

  final val ExampleGameStartTime = Instant.ofEpochMilli(1577912400000L)

  final val ExampleGame = Game(ExampleGameId, Some(ExampleGameDescription), ExampleUserId, Some(ExampleGameStartTime), None)

  final val ExampleNotStartedGame = ExampleGame.copy(startedAt = None)

  final val ExamplePlayer = Player(ExampleUserId, ExampleNickName, Some(PlayerRole.Player))

  final val InitClockMillis = 1602714542868L

  final val Now = Instant.ofEpochMilli(1602795725091L)

}
