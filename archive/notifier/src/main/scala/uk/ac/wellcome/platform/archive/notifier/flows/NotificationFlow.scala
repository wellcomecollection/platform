package uk.ac.wellcome.platform.archive.notifier.flows

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Merge}
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishResult
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.progress.models.{Progress, ProgressUpdate}
import uk.ac.wellcome.platform.archive.notifier.flows.callback.{WithCallbackUrlFlow, WithoutCallbackUrlFlow}
import uk.ac.wellcome.platform.archive.notifier.flows.notification.PrepareNotificationFlow
import uk.ac.wellcome.platform.archive.notifier.models.CallbackFlowResult
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow

/** This flow receives a [[Progress]] instance, as received from
  * an SQS queue, and makes the callback request (if necessary).
  *
  * After this flow has done, we need to know two things:
  *   - The original Progress object
  *   - Whether the callback succeeded (if necessary) -- we record
  * the callback result.
  *
  */
object NotificationFlow {
  def apply(snsClient: AmazonSNS, snsConfig: SNSConfig)(implicit actorSystem: ActorSystem)
  : Flow[Progress, PublishResult, NotUsed] = {

    val withCallbackUrlFlow = WithCallbackUrlFlow()
    val withoutCallbackUrlFlow = WithoutCallbackUrlFlow()
    val prepareNotificationFlow = PrepareNotificationFlow()
    val snsPublishFlow = SnsPublishFlow[ProgressUpdate](snsClient, snsConfig)

    // This creates a custom Akka Graph, and splits Progress objects
    // based on whether they have a callback URL.
    //
    //                              Broadcast                     Merge
    //
    //                  +-----> withCallbackUrlFlow     ~~~> [CallbackFlowResult]
    //                  |       (has a callback URL)
    //    [Progress] ---+
    //                  |
    //                  +-----> withoutCallbackUrlFlow  ~~~> [CallbackFlowResult]
    //                          (no callback URL)
    //
    // How it works:
    //
    //  1.  Every instance of Progress sent to the original flow is duplicated,
    //      and sent to both `withCallbackUrlFlow` and `withoutCallbackUrlFlow`.
    //      (This is the `Broadcast` component.)
    //
    //  2.  Each flow filters out half the messages, so each Progress is handled
    //      by exactly once flow.
    //
    //  3.  Each flow processes Progress messages and turns them into instances of
    //      CallbackFlowResult.
    //
    //  4.  The result is two flows that both take [Progress ~> CallbackFlowResult].
    //      (This is the `Merge` component.)
    //
    Flow.fromGraph(
      GraphDSL.create(Broadcast[Progress](2), Merge[CallbackFlowResult](2))(
        Keep.none) { implicit builder =>
        import GraphDSL.Implicits._
        (broadcast, merge) => {
          val withCallbackUrl = builder.add(withCallbackUrlFlow)
          val withoutCallbackUrl = builder.add(withoutCallbackUrlFlow)
          val prepareNotification = builder.add(prepareNotificationFlow)
          val snsPublish = builder.add(snsPublishFlow)

          broadcast.out(0) ~> withCallbackUrl ~> merge.in(0)
          broadcast.out(1) ~> withoutCallbackUrl ~> merge.in(1)

          merge.out ~> prepareNotification ~> snsPublish

          FlowShape(broadcast.in, snsPublish.out)
        }
      })
  }
}
