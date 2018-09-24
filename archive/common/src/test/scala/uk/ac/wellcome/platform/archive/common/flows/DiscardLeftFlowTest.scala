package uk.ac.wellcome.platform.archive.common.flows

import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.platform.archive.common.models.NotificationMessage
import uk.ac.wellcome.test.fixtures.Akka
import uk.ac.wellcome.test.utils.ExtendedPatience

class NotificationParsingFlowTest
  extends FunSpec
    with Akka
    with Matchers
    with ExtendedPatience
    with ScalaFutures {

  import uk.ac.wellcome.json.JsonUtil._

  it("parses T from a message body") {
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { materializer =>
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
          .map(body => NotificationMessage(
              MessageId = "MessageId",
              TopicArn = "TopicArn",
              Subject = None,
              Message = body.toString
            ))

        val source = Source(messages)
        val parsingFlow = NotificationParsingFlow[Character]

        print(messages)

        val eventualResult = source
          .via(parsingFlow)
          .async
          .runWith(Sink.seq)(materializer)

        whenReady(eventualResult) { result =>
          result.toList shouldBe characters
        }

      }
    }
  }
}

class DiscardLeftFlowTest
  extends FunSpec
    with Akka
    with Matchers
    with ExtendedPatience
    with ScalaFutures {

  it("discards Left values") {
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { materializer =>
        val leftList = List("fail", "flumps").map(Left(_))

        val sourceList = List("boomer", "bust", "banana")
        val rightList = sourceList.map(Right(_))

        val list: List[Either[String, String]] = leftList ++ rightList

        val source = Source(list)
        val discardLeftFlow = DiscardLeftFlow[String, String]()

        val eventualResult = source
          .via(discardLeftFlow)
          .async
          .runWith(Sink.seq)(materializer)

        whenReady(eventualResult) { result =>
          result.toList shouldBe sourceList
        }
      }
    }
  }
}
