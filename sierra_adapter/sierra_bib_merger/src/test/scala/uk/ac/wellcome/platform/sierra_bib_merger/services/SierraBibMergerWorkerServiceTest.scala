package uk.ac.wellcome.platform.sierra_bib_merger.services

import akka.actor.ActorSystem
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.sierra_adapter.dynamo.MergedSierraRecordDao
import uk.ac.wellcome.sqs.{SQSReader, SQSReaderGracefulException}
import uk.ac.wellcome.test.utils.ExtendedPatience

class SierraBibMergerWorkerServiceTest
    extends FunSpec
    with MockitoSugar
    with ScalaFutures
    with Matchers
    with ExtendedPatience {

  it("does not throw a NullPointerException when receiving a message with null body") {

    val sqsReader = mock[SQSReader]
    val metricsSender = mock[MetricsSender]
    val mergerUpdaterService =
      new SierraBibMergerUpdaterService(mock[MergedSierraRecordDao],
                                        metricsSender)
    val worker = new SierraBibMergerWorkerService(sqsReader,
                                                  ActorSystem(),
                                                  metricsSender,
                                                  mergerUpdaterService)

    val future = worker.processMessage(
      SQSMessage(subject = Some("default-subject"),
                 body = "null",
                 topic = "",
                 messageType = "",
                 timestamp = ""))

    whenReady(future.failed) { ex =>
      ex shouldBe a[SQSReaderGracefulException]
    }

  }
}
