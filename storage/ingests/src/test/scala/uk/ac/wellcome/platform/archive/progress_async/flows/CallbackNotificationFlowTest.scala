package uk.ac.wellcome.platform.archive.progress_async.flows

import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.fixtures.SNS
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.CallbackNotification
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressGenerators
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Callback,
  Progress
}
import uk.ac.wellcome.test.fixtures.Akka
import uk.ac.wellcome.json.JsonUtil._

class CallbackNotificationFlowTest
    extends FunSpec
    with Akka
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with Eventually
    with RandomThings
    with ProgressGenerators
    with SNS {

  it("sends callbackNotifications") {
    val sendsCallbackStatus = Table(
      ("progress-status", "callback-status"),
      (Progress.Failed, Callback.Pending),
      (Progress.Completed, Callback.Pending)
    )
    forAll(sendsCallbackStatus) { (progressStatus, callbackStatus) =>
      withMaterializer { implicit materializer =>
        withLocalSnsTopic { topic =>
          val callbackNotificationFlow = CallbackNotificationFlow(
            snsClient,
            snsConfig = createSNSConfigWith(topic)
          )

          val progress = createProgressWith(
            status = progressStatus,
            callback = Some(createCallbackWith(status = callbackStatus)))

          val eventuallyResult = Source
            .single(progress)
            .via(callbackNotificationFlow)
            .runWith(Sink.seq)

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

  it("doesn't send callbackNotifications") {
    val doesNotSendCallbackStatus = Table(
      ("progress-status", "callback-status"),
      (Progress.Accepted, Callback.Pending),
      (Progress.Accepted, Callback.Succeeded),
      (Progress.Accepted, Callback.Failed),
      (Progress.Processing, Callback.Pending),
      (Progress.Processing, Callback.Succeeded),
      (Progress.Processing, Callback.Failed),
      (Progress.Failed, Callback.Succeeded),
      (Progress.Failed, Callback.Failed),
      (Progress.Completed, Callback.Succeeded),
      (Progress.Completed, Callback.Failed)
    )
    withMaterializer { implicit materializer =>
      withLocalSnsTopic { topic =>
        forAll(doesNotSendCallbackStatus) { (progressStatus, callbackStatus) =>
          val callbackNotificationFlow = CallbackNotificationFlow(
            snsClient,
            snsConfig = createSNSConfigWith(topic)
          )

          val progress = createProgressWith(
            status = progressStatus,
            callback = Some(createCallbackWith(status = callbackStatus)))

          val eventuallyResult = Source
            .single(progress)
            .via(callbackNotificationFlow)
            .runWith(Sink.seq)

          whenReady(eventuallyResult) { result =>
            result should have size 1
            assertSnsReceivesNothing(topic)
          }
        }
      }
    }
  }
}
