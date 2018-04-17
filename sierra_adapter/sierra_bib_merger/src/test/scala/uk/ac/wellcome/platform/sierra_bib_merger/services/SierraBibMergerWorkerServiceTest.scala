package uk.ac.wellcome.platform.sierra_bib_merger.services

import akka.actor.ActorSystem
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.sqs.SQSReader
import uk.ac.wellcome.storage.VersionedHybridStore
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.test.fixtures.S3

class SierraBibMergerWorkerServiceTest
    extends FunSpec
    with MockitoSugar
    with ScalaFutures
    with Matchers
    with ExtendedPatience
    with S3 {

  it(
    "throws a GracefulFailureException if the message on the queue does not represent a SierraRecord") {

    val sqsReader = mock[SQSReader]
    val metricsSender = mock[MetricsSender]
    val mergerUpdaterService =
      new SierraBibMergerUpdaterService(
        mock[VersionedHybridStore[SierraTransformable]],
        metricsSender)
    val worker = new SierraBibMergerWorkerService(
      sqsReader,
      ActorSystem(),
      metricsSender,
      mergerUpdaterService,
      s3Client)

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
