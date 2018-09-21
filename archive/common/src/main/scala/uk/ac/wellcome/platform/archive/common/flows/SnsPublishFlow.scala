package uk.ac.wellcome.platform.archive.common.flows

import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Zip}
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.{PublishRequest, PublishResult}
import grizzled.slf4j.Logging
import io.circe.Encoder
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.progress.models.FailedEvent

import scala.util.Try

object SnsPublishFlow extends Logging {

  def apply[T](
                   snsClient: AmazonSNS, snsConfig: SNSConfig,
                   subject: String = ""
                 )(implicit encode: Encoder[T]) = {

    def toPublishRequest(message: String) =
      new PublishRequest(snsConfig.topicArn, message, subject)

    val prepareNoticeFlow = Flow[T]
      .map(toJson[T](_).toEither)

    val publishNoticeFlow = Flow[String]
      .map(toPublishRequest)
      .map(request => Try(snsClient.publish(request)))
      .map(_.toEither)

    val eitherEventFlow = Flow[(Option[Throwable], T)].map {
      case (Some(e), t) => Left(FailedEvent(e, t))
      case (None, t) => Right(t)
    }

    Flow.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val inT = builder.add(Flow[T])
      val broadcastT = builder.add(Broadcast[T](2))
      val prepareNotice = builder.add(prepareNoticeFlow)
      val publishNotice = builder.add(publishNoticeFlow)

      val e1 = builder.add(EitherFanOutFlow[Throwable, String]())
      val e2 = builder.add(EitherFanOutFlow[Throwable, PublishResult]())

      val merge = builder.add(Merge[Option[Throwable]](3))

      val zip = builder.add(Zip[Option[Throwable], T])
      val eitherEvent = builder.add(eitherEventFlow)

      // Broadcast T so that we can use it later
      inT ~> broadcastT

      // Prepare notice and split in EitherFanOutFlow (e1)
      broadcastT.out(0) ~> prepareNotice ~> e1.in

      // Right hand outlet continues to the next EitherFanOutFlow (e2)
      e1.out1 ~> publishNotice ~> e2.in

      // Merge Option of Throwable from EitherFanOutFlow
      e1.out0.map(Some(_)) ~> merge.in(0)
      e2.out0.map(Some(_)) ~> merge.in(1)
      e2.out1.map(_ => None) ~> merge.in(2)

      // Pass Option of Throwable to left hand side of Zip
      merge.out ~> zip.in0

      // Right hand side of Zip joins T
      broadcastT.out(1) ~> zip.in1

      // Convert zip to either
      zip.out ~> eitherEvent.in

      FlowShape(inT.in, eitherEvent.out)
    })
  }
}