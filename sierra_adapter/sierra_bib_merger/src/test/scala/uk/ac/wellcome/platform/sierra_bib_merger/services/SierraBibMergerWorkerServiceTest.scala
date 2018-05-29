package uk.ac.wellcome.platform.sierra_bib_merger.services

import akka.actor.ActorSystem
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.messaging.sqs.{SQSMessage, SQSReader}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.storage.s3.S3TypeStore
import uk.ac.wellcome.storage.vhs.{SourceMetadata, VersionedHybridStore}
import uk.ac.wellcome.test.utils.ExtendedPatience

class SierraBibMergerWorkerServiceTest
    extends FunSpec
    with MockitoSugar
    with ScalaFutures
    with Matchers
    with ExtendedPatience {

  it(
    "throws a GracefulFailureException if the message on the queue does not represent a SierraRecord") {

    val sqsReader = mock[SQSReader]
    val metricsSender = mock[MetricsSender]
    val mergerUpdaterService =
      new SierraBibMergerUpdaterService(
        mock[VersionedHybridStore[SierraTransformable,
                                  SourceMetadata,
                                  S3TypeStore[SierraTransformable]]],
        metricsSender)
    val worker = new SierraBibMergerWorkerService(
      sqsReader,
      ActorSystem(),
      metricsSender,
      mergerUpdaterService)

    val future = worker.processMessage(
      SQSMessage(
        subject = Some("default-subject"),
        body = "null",
        topic = "",
        messageType = "",
        timestamp = ""))

    whenReady(future.failed) { ex =>
      ex shouldBe a[GracefulFailureException]
    }

  }
}
