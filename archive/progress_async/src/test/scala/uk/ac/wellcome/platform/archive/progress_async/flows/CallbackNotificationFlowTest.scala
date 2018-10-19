package uk.ac.wellcome.platform.archive.progress_async.flows

import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.test.fixtures.SNS
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.json.{
  URIConverters,
  UUIDConverters
}
import uk.ac.wellcome.platform.archive.common.models.CallbackNotification
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressGenerators
import uk.ac.wellcome.platform.archive.common.progress.models.progress.{
  Callback,
  Progress
}
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
    with ProgressGenerators
    with SNS {

  it("sends callbackNotifications") {
    val sendsCallbackStatus = Table(
      ("progress-status", "callback-status"),
      (Progress.Failed, Callback.Pending),
      (Progress.Completed, Callback.Pending)
    )
    forAll(sendsCallbackStatus) { (progressStatus, callbackStatus) =>
      withActorSystem { actorSystem =>
        withMaterializer(actorSystem) { materializer =>
          withLocalSnsTopic { topic =>
            val callbackNotificationFlow =
              CallbackNotificationFlow(snsClient, SNSConfig(topic.arn))

            val progress = createProgressWith(
              status = progressStatus,
              callback = Some(createCallbackWith(status = callbackStatus)))

            val eventuallyResult = Source
              .single(progress)
              .via(callbackNotificationFlow)
              .runWith(Sink.seq)(materializer)

            whenReady(eventuallyResult) { result =>
              result should have size 1
              val msg = notificationMessage[CallbackNotification](topic)
              msg.id shouldBe progress.id
              msg.callbackUri shouldBe progress.callback.get.uri
              msg.payload shouldBe progress
            }
          }
        }
      }
    }
  }

  it("doesn't send callbackNotifications") {
    val doesNotSendCallbackStatus = Table(
      ("progress-status", "callback-status"),
      (Progress.Initialised, Callback.Pending),
      (Progress.Initialised, Callback.Succeeded),
      (Progress.Initialised, Callback.Failed),
      (Progress.Processing, Callback.Pending),
      (Progress.Processing, Callback.Succeeded),
      (Progress.Processing, Callback.Failed),
      (Progress.Failed, Callback.Succeeded),
      (Progress.Failed, Callback.Failed),
      (Progress.Completed, Callback.Succeeded),
      (Progress.Completed, Callback.Failed)
    )
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { materializer =>
        withLocalSnsTopic { topic =>
          forAll(doesNotSendCallbackStatus) {
            (progressStatus, callbackStatus) =>
              val callbackNotificationFlow =
                CallbackNotificationFlow(snsClient, SNSConfig(topic.arn))

              val progress = createProgressWith(
                status = progressStatus,
                callback = Some(createCallbackWith(status = callbackStatus)))

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

}
