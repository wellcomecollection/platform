package uk.ac.wellcome.platform.archive.common.progress.flows

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Zip}
import com.amazonaws.services.sns.AmazonSNS
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.flows.{
  EitherFanOutFlow,
  SnsPublishFlow
}
import uk.ac.wellcome.platform.archive.common.progress.models.{
  FailedEvent,
  Progress,
  ProgressUpdate
}
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressMonitor

object ProgressUpdateAndPublishFlow {

  type FailedUpdate = FailedEvent[ProgressUpdate]
  type FailedPublish = FailedEvent[Progress]

  def apply(
    snsClient: AmazonSNS,
    snsConfig: SNSConfig,
    progressMonitor: ProgressMonitor
  ): Flow[ProgressUpdate,
          Either[FailedEvent[ProgressUpdate], ProgressUpdate],
          NotUsed] = {

    val progressUpdateFlow = ProgressUpdateFlow(progressMonitor)
    val publishFlow = SnsPublishFlow[Progress](snsClient, snsConfig)
    val mergeFlow = Merge[Option[Throwable]](3)

    val eitherEventFlow = Flow[(Option[Throwable], ProgressUpdate)].map {
      case (Some(e), t) => Left(FailedEvent(e, t))
      case (None, t)    => Right(t)
    }

    val wrapFailedUpdate = Flow[FailedUpdate].map(failedUpdate => {
      Some(failedUpdate.e)
    })

    val wrapFailedPublish = Flow[FailedPublish].map(failedPublish => {
      Some(failedPublish.e)
    })

    Flow.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val update = builder.add(Flow[ProgressUpdate])
      val broadcast = builder.add(Broadcast[ProgressUpdate](2))
      val store = builder.add(progressUpdateFlow)
      val e1 = builder.add(EitherFanOutFlow[FailedUpdate, Progress]())
      val e2 = builder.add(EitherFanOutFlow[FailedPublish, Progress]())
      val publish = builder.add(publishFlow)
      val failedUpdate = builder.add(wrapFailedUpdate)
      val failedPublish = builder.add(wrapFailedPublish)
      val success = builder.add(Flow[Progress].map(_ => None))
      val eitherEvent = builder.add(eitherEventFlow)
      val merge = builder.add(mergeFlow)
      val zip = builder.add(Zip[Option[Throwable], ProgressUpdate])

      // Broadcast ProgressUpdate so we can use it later
      update ~> broadcast.in

      // Store ProgressUpdate, pass Either to e1
      broadcast.out(0) ~> store ~> e1.in

      // Right of Store ProgressUpdate passed to Publish
      e1.out1 ~> publish

      // Publish result, pass Either to e2
      publish ~> e2.in

      // Wrap results in Left/Right as necessary
      e1.out0 ~> failedUpdate
      e2.out0 ~> failedPublish
      e2.out1 ~> success

      // Merge all Left/Right outlets
      failedUpdate ~> merge.in(0)
      failedPublish ~> merge.in(1)
      success ~> merge.in(2)

      // Pass Option of Throwable to left hand side of Zip
      merge.out ~> zip.in0

      // Right hand side of Zip joins ProgressUpdate
      broadcast.out(1) ~> zip.in1

      // Convert zip to either
      zip.out ~> eitherEvent.in

      FlowShape(update.in, eitherEvent.out)
    })
  }
}
