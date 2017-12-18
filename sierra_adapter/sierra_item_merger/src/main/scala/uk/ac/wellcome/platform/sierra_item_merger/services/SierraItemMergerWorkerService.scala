package uk.ac.wellcome.platform.sierra_item_merger.services

import java.time.Instant

import akka.actor.ActorSystem
import cats.syntax.either._
import com.google.inject.Inject
import grizzled.slf4j.Logging
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import io.circe.parser._
import io.circe.{Decoder, HCursor}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.SierraItemRecord
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.sqs.{SQSReader, SQSReaderGracefulException, SQSWorker}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SierraItemMergerWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  sierraItemMergerUpdaterService: SierraItemMergerUpdaterService
) extends SQSWorker(reader, system, metrics)
    with Logging {

  // Using Circe here because Jackson creates nulls for empty lists
  implicit val decodeInstant: Decoder[Instant] = new Decoder[Instant] {
    final def apply(c: HCursor): Decoder.Result[Instant] =
      for {
        epochSeconds <- c.as[Long]
      } yield {
        Instant.ofEpochSecond(epochSeconds)
      }
  }

  implicit val customConfig: Configuration =
    Configuration.default.withDefaults.withDiscriminator("type")

  override def processMessage(message: SQSMessage): Future[Unit] =
    decode[SierraItemRecord](message.body) match {
      case Right(record) => sierraItemMergerUpdaterService.update(record)
      case Left(e) =>
        Future {
          logger.warn(s"Failed processing $message", e)
          throw SQSReaderGracefulException(e)
        }
    }
}
