package poker.protocol

import io.circe.Json
import io.circe.parser.decode
import io.circe.syntax.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import poker.domain.GameSettings
import poker.protocol.JsonCodecs.given

class ProtocolSpec extends AnyFlatSpec with Matchers {
  "ClientMessage JSON encoder" should "encode Identify without playerId" in {
    val msg: ClientMessage = ClientMessage.Identify("Alice", None)
    val json = msg.asJson

    json shouldBe Json.obj(
      "type" -> Json.fromString("Identify"),
      "name" -> Json.fromString("Alice"),
      "playerId" -> Json.Null
    )
  }

  it should "encode Identify with playerId" in {
    val msg: ClientMessage = ClientMessage.Identify("Alice", Some("p1"))
    val json = msg.asJson

    json shouldBe Json.obj(
      "type" -> Json.fromString("Identify"),
      "name" -> Json.fromString("Alice"),
      "playerId" -> Json.fromString("p1")
    )
  }

  it should "encode CreateGame with settings" in {
    val msg: ClientMessage = ClientMessage.CreateGame(
      GameSettings(
        name = "",
        smallBlind = 10,
        bigBlind = 20,
        initialChips = 1000,
        isPublic = true,
        maxPlayers = 6
      )
    )
    val json = msg.asJson

    json shouldBe Json.obj(
      "type" -> Json.fromString("CreateGame"),
      "settings" -> Json.obj(
        "name" -> Json.fromString(""),
        "smallBlind" -> Json.fromInt(10),
        "bigBlind" -> Json.fromInt(20),
        "initialChips" -> Json.fromInt(1000),
        "isPublic" -> Json.fromBoolean(true),
        "maxPlayers" -> Json.fromInt(6)
      )
    )
  }

  it should "encode game action messages" in {
    val msg1: ClientMessage = ClientMessage.StartGame("abc")
    msg1.asJson shouldBe Json.obj(
      "type" -> Json.fromString("StartGame"),
      "code" -> Json.fromString("abc")
    )

    val msg2: ClientMessage = ClientMessage.Fold("abc")
    msg2.asJson shouldBe Json.obj(
      "type" -> Json.fromString("Fold"),
      "code" -> Json.fromString("abc")
    )

    val msg3: ClientMessage = ClientMessage.Check("abc")
    msg3.asJson shouldBe Json.obj(
      "type" -> Json.fromString("Check"),
      "code" -> Json.fromString("abc")
    )

    val msg4: ClientMessage = ClientMessage.Call("abc")
    msg4.asJson shouldBe Json.obj(
      "type" -> Json.fromString("Call"),
      "code" -> Json.fromString("abc")
    )

    val msg5: ClientMessage = ClientMessage.Raise("abc", 50)
    msg5.asJson shouldBe Json.obj(
      "type" -> Json.fromString("Raise"),
      "code" -> Json.fromString("abc"),
      "amount" -> Json.fromInt(50)
    )

    val msg6: ClientMessage = ClientMessage.LeaveGame("abc")
    msg6.asJson shouldBe Json.obj(
      "type" -> Json.fromString("LeaveGame"),
      "code" -> Json.fromString("abc")
    )

    val msg7: ClientMessage = ClientMessage.Ping
    msg7.asJson shouldBe Json.obj(
      "type" -> Json.fromString("Ping")
    )
  }

  "ClientMessage JSON decoder" should "decode valid Raise JSON" in {
    val json = """{"type":"Raise","code":"abc","amount":50}"""

    decode[ClientMessage](json) shouldBe Right(
      ClientMessage.Raise("abc", 50)
    )
  }

  it should "decode valid CreateGame JSON" in {
    val json =
      """
        {
          "type": "CreateGame",
          "settings": {
            "name": "",
            "smallBlind": 10,
            "bigBlind": 20,
            "initialChips": 1000,
            "isPublic": true,
            "maxPlayers": 6
          }
        }
      """

    decode[ClientMessage](json) shouldBe Right(
      ClientMessage.CreateGame(
        GameSettings(
          name = "",
          smallBlind = 10,
          bigBlind = 20,
          initialChips = 1000,
          isPublic = true,
          maxPlayers = 6
        )
      )
    )
  }

  it should "reject unknown message type" in {
    val json = """{"type":"Unknown"}"""

    decode[ClientMessage](json).isLeft shouldBe true
  }

  it should "reject Raise without amount" in {
    val json = """{"type":"Raise","code":"abc"}"""

    decode[ClientMessage](json).isLeft shouldBe true
  }

  it should "reject JoinGame without code" in {
    val json = """{"type":"JoinGame"}"""

    decode[ClientMessage](json).isLeft shouldBe true
  }
}

