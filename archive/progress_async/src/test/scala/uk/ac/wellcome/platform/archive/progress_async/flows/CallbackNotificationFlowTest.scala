package uk.ac.wellcome.platform.archive.progress_async.flows

import java.net.URI

import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.test.fixtures.SNS
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.json.{URIConverters, UUIDConverters}
import uk.ac.wellcome.platform.archive.common.models.CallbackNotification
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.platform.archive.common.progress.models.progress.Namespace
import uk.ac.wellcome.test.fixtures.Akka

class CallbackNotificationFlowTest
  extends FunSpec
    with Akka
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with Eventually
    with RandomThings
    with UUIDConverters
    with URIConverters
    with SNS {

  val space = Namespace("space-id")
  val uploadUri = new URI("http://www.example.com/asset")
  val callbackUri = new URI("http://localhost/archive/complete")

  it("sends callbackNotifications") {
    val status = Table(
      "status",
      Progress.Completed,
      Progress.Failed
    )
    forAll(status) { status =>
      withActorSystem { actorSystem =>
        withMaterializer(actorSystem) { materializer =>
          withLocalSnsTopic { topic =>
            val callbackNotificationFlow =
              CallbackNotificationFlow(snsClient, SNSConfig(topic.arn))
            val id = randomUUID
            val progress =
              Progress(id, uploadUri, Some(callbackUri), space, status)

            val eventuallyResult = Source
              .single(progress)
              .via(callbackNotificationFlow)
              .runWith(Sink.seq)(materializer)

            whenReady(eventuallyResult) { result =>
              result should have size (1)
              val msg = notificationMessage[CallbackNotification](topic)
              msg.id shouldBe id
              msg.callbackUri shouldBe callbackUri
              msg.payload shouldBe progress
            }
          }
        }
      }
    }
  }

  it("doesn't send callbackNotifications") {
    val status = Table(
      "status",
      Progress.CompletedCallbackFailed,
      Progress.CompletedCallbackSucceeded,
      Progress.None
    )
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { materializer =>
        withLocalSnsTopic { topic =>
          forAll(status) { status =>
            val callbackNotificationFlow =
              CallbackNotificationFlow(snsClient, SNSConfig(topic.arn))
            val id = randomUUID

            val progress =
              Progress(id, uploadUri, Some(callbackUri), space, status)

            val eventuallyResult = Source
              .single(progress)
              .via(callbackNotificationFlow)
              .runWith(Sink.seq)(materializer)

            whenReady(eventuallyResult) { result =>
              result should have size 1
              assertSnsReceivesNothing(topic)
            }
          }
        }
      }
    }
  }

  it("failure to send to callbackNotifications ends up on the DLQ") {
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { materializer =>
        val callbackNotificationFlow =
          CallbackNotificationFlow(snsClient, SNSConfig("does-not-exist"))
        val id = randomUUID
        val progress =
          Progress(id, uploadUri, Some(callbackUri), space, Progress.Completed)

        val eventuallyResult = Source
          .single(progress)
          .via(callbackNotificationFlow)
          .runWith(Sink.seq)(materializer)

        whenReady(eventuallyResult) { result =>
          result shouldBe List.empty
        }
      }
    }
  }
}
