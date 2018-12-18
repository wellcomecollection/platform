package uk.ac.wellcome.platform.archive.common.flows

import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.services.sns.model.PublishResult
import org.scalatest.FunSpec
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.SNS
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.test.fixtures.Akka

class SNSPublishFlowTest
    extends FunSpec
    with SNS
    with MockitoSugar
    with Akka
    with ScalaFutures
    with IntegrationPatience {

  case class Person(name: String, age: Int)

  it("publishes a notification and returns Right(T)") {
    withLocalSnsTopic { topic =>
      withMaterializer { implicit materializer =>
        val bob = Person("Bobbert", 42)

        val snsConfig = createSNSConfigWith(topic)
        val publishFlow =
          SnsPublishFlow[Person](
            snsClient,
            snsConfig,
            subject = "SNSPublishFlowTest")

        val publication = Source
          .single(bob)
          .via(publishFlow)
          .async
          .runWith(Sink.head)

        whenReady(publication) { result =>
          result shouldBe a[PublishResult]

          assertSnsReceivesOnly(bob, topic)
        }
      }
    }
  }

  it("handles multiple errors") {
    withMaterializer { implicit materializer =>
      val bobs = () =>
        Iterator(
          Person("Bobbert", 42),
          Person("Bobbrit", 41),
          Person("Borbbit", 40)
      )

      val snsConfig = createSNSConfigWith(Topic("bad_topic"))
      val publishFlow =
        SnsPublishFlow[Person](
          snsClient,
          snsConfig,
          subject = "SNSPublishFlowTest")

      val publication = Source
        .fromIterator(bobs)
        .via(publishFlow)
        .async
        .runWith(Sink.seq)

      whenReady(publication) { result =>
        result shouldBe empty
      }
    }
  }

  it("publishes multiple notifications") {
    withLocalSnsTopic { topic =>
      withMaterializer { implicit materializer =>
        val bobs = () =>
          Iterator(
            Person("Bobbert", 42),
            Person("Bobbrit", 41),
            Person("Borbbit", 40)
        )

        val snsConfig = createSNSConfigWith(topic)
        val publishFlow =
          SnsPublishFlow[Person](
            snsClient,
            snsConfig,
            subject = "SNSPublishFlowTest")

        val publication = Source
          .fromIterator(bobs)
          .via(publishFlow)
          .async
          .runWith(Sink.seq)

        val resultantBobs = bobs().toList

        whenReady(publication) { result =>
          result should have length resultantBobs.length

          assertSnsReceives(resultantBobs, topic)
        }

      }
    }
  }
}
