package uk.ac.wellcome.sqs

import akka.actor.ActorSystem
import org.mockito.Matchers.{anyDouble, anyString}
import org.mockito.Mockito
import org.mockito.Mockito.{never, verify}
import org.scalatest.FunSpec
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{SQSConfig, SQSMessage}
import uk.ac.wellcome.test.utils.SQSLocal

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.collection.JavaConversions._

class SQSWorkerTest extends FunSpec with SQSLocal with MockitoSugar{

  val queueUrl = createQueueAndReturnUrl("worker-test-queue")
  private val metricsSender: MetricsSender = mock[MetricsSender]
  sqsClient.setQueueAttributes(queueUrl, Map("VisibilityTimeout" -> "0"))

  val worker = new SQSWorker(new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1)), ActorSystem(), metricsSender) {
    override lazy val totalWait = 1.second

    override def processMessage(message: SQSMessage): Future[Unit] = Future.successful(())
  }

  it("should not fail the TryBackoff run if it's unable to parse a message into SQSMessage"){
    worker.runSQSWorker()
    sqsClient.sendMessage(queueUrl, "this is not valid Json")

    Thread.sleep(worker.totalWait.toMillis + 1000)

    verify(metricsSender, never()).incrementCount(anyString(), anyDouble())
  }
}
