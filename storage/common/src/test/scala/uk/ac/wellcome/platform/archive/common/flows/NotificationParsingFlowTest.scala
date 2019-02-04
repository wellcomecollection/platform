package uk.ac.wellcome.platform.archive.common.flows

import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import uk.ac.wellcome.test.fixtures.Akka
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.platform.archive.common.messaging.NotificationParsingFlow

class NotificationParsingFlowTest
    extends FunSpec
    with Akka
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  it("parses T from a message body") {
    withMaterializer { implicit materializer =>
      case class Character(id: String, age: Int)

      val characters = List(
        Character("Dave", 34),
        Character("Cat", 7),
        Character("Arnold", 32),
        Character("Holly", 3528591)
      )

      val jsonStrings = characters.map(toJson[Character](_).get)
      val badStrings = List("gazpacho soup")

      val messages = jsonStrings
        .patch(2, badStrings, 0)
        .map(body => NotificationMessage(body = body.toString))

      val source = Source(messages)
      val parsingFlow = NotificationParsingFlow[Character]

      print(messages)

      val eventualResult = source
        .via(parsingFlow)
        .async
        .runWith(Sink.seq)

      whenReady(eventualResult) { result =>
        result.toList shouldBe characters
      }
    }
  }
}
