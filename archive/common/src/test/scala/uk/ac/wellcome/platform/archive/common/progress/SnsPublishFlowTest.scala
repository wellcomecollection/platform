package uk.ac.wellcome.platform.archive.common.progress

import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.services.sns.model.AmazonSNSException
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.test.fixtures.SNS
import uk.ac.wellcome.platform.archive.common.flows.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.progress.models.FailedEvent
import uk.ac.wellcome.test.fixtures.Akka
import uk.ac.wellcome.test.utils.ExtendedPatience

class SNSPublishFlowTest
  extends FunSpec
    with SNS
    with MockitoSugar
    with Akka
    with ExtendedPatience
    with ScalaFutures {

  case class Person(name: String, age: Int)

  it("publishes a notification and returns Right(T)") {
    withLocalSnsTopic { topic =>
      withActorSystem { actorSystem =>
        withMaterializer(actorSystem) { materializer =>
          val bob = Person("Bobbert", 42)

          val snsConfig = SNSConfig(topic.arn)
          val publishFlow =
            SnsPublishFlow[Person](snsClient, snsConfig)

          val publication = Source.single(bob)
            .via(publishFlow)
            .async
            .runWith(Sink.head)(materializer)

          whenReady(publication) { result =>
            result.isRight shouldBe true
            result.right.get shouldBe bob
          }

          assertSnsReceivesOnly(bob, topic)
        }
      }
    }
  }

  it("returns a Left(FailedEvent[T]) on failure") {
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { materializer =>
        val bob = Person("Bobbert", 42)

        val snsConfig = SNSConfig("bad_topic")
        val publishFlow =
          SnsPublishFlow[Person](snsClient, snsConfig)

        val publication = Source.single(bob)
          .via(publishFlow)
          .async
          .runWith(Sink.head)(materializer)

        whenReady(publication) { result =>
          result.isLeft shouldBe true
          result.left.get shouldBe a[FailedEvent[_]]
          result.left.get.t shouldBe bob
          result.left.get.e shouldBe a[AmazonSNSException]
        }
      }
    }
  }

  it("handles multiple errors, returning Lefts") {
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { materializer =>

        val bobs = () => Iterator(
          Person("Bobbert", 42),
          Person("Bobbrit", 41),
          Person("Borbbit", 40)
        )

        val snsConfig = SNSConfig("bad_topic")
        val publishFlow =
          SnsPublishFlow[Person](snsClient, snsConfig)

        val publication = Source.fromIterator(bobs)
          .via(publishFlow)
          .async
          .runWith(Sink.seq)(materializer)

        val resultantBobs = bobs().toList

        whenReady(publication) { result =>
          result.collect {
            case Left(FailedEvent(_: AmazonSNSException, t)) => t
          } shouldBe resultantBobs
        }
      }
    }
  }

  it("publishes multiple notifications") {
    withLocalSnsTopic { topic =>
      withActorSystem { actorSystem =>
        withMaterializer(actorSystem) { materializer =>

          val bobs = () => Iterator(
            Person("Bobbert", 42),
            Person("Bobbrit", 41),
            Person("Borbbit", 40)
          )

          val snsConfig = SNSConfig(topic.arn)
          val publishFlow =
            SnsPublishFlow[Person](snsClient, snsConfig)

          val publication = Source.fromIterator(bobs)
            .via(publishFlow)
            .async
            .runWith(Sink.seq)(materializer)

          val resultantBobs = bobs().toList

          whenReady(publication) { result =>
            result shouldBe resultantBobs.map(Right(_))
          }

          assertSnsReceives(resultantBobs, topic)
        }
      }
    }
  }
}

