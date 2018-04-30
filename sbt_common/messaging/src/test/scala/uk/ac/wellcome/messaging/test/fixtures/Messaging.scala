package uk.ac.wellcome.messaging.test.fixtures

import akka.actor.ActorSystem
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sns.model.{SubscribeRequest, SubscribeResult}
import io.circe.Decoder
import io.circe._
import io.circe.generic.semiauto._
import uk.ac.wellcome.messaging.message.{MessageReader, MessageWorker}
import uk.ac.wellcome.messaging.sqs.SQSReader
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.metrics
import uk.ac.wellcome.models.aws.{S3Config, SQSConfig}
import uk.ac.wellcome.s3.{KeyPrefixGenerator, S3ObjectStore}
import uk.ac.wellcome.test.fixtures.{
  Akka,
  ImplicitLogging,
  MetricsSender,
  TestWith
}
import uk.ac.wellcome.test.fixtures.S3.Bucket

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Future
import com.amazonaws.services.sns.model.UnsubscribeRequest

trait Messaging
    extends Akka
    with MetricsSender
    with SQS
    with SNS
    with S3
    with ImplicitLogging {

  def withLocalStackSubscription[R](queue: Queue, topic: Topic) =
    fixture[SubscribeResult, R](
      create = {
        val subRequest = new SubscribeRequest(topic.arn, "sqs", queue.arn)
        info(s"Subscribing queue ${queue.arn} to topic ${topic.arn}")

        localStackSnsClient.subscribe(subRequest)
      },
      destroy = { subscribeResult =>
        val unsubscribeRequest =
          new UnsubscribeRequest(subscribeResult.getSubscriptionArn)
        localStackSnsClient.unsubscribe(unsubscribeRequest)
      }
    )

  case class ExampleObject(name: String)

  class ExampleMessageWorker(
    sqsReader: SQSReader,
    messageReader: MessageReader[ExampleObject],
    actorSystem: ActorSystem,
    metricsSender: metrics.MetricsSender
  ) extends MessageWorker[ExampleObject](
        sqsReader,
        messageReader,
        actorSystem,
        metricsSender
      ) {

    var calledWith: Option[ExampleObject] = None
    def hasBeenCalled: Boolean = calledWith.nonEmpty

    override implicit val decoder: Decoder[ExampleObject] =
      deriveDecoder[ExampleObject]

    override def processMessage(message: ExampleObject) = Future {
      calledWith = Some(message)

      info("processMessage was called!")
    }
  }

  val keyPrefixGenerator: KeyPrefixGenerator[ExampleObject] =
    new KeyPrefixGenerator[ExampleObject] {
      override def generate(obj: ExampleObject): String = "/"
    }

  def withMessageReader[R](bucket: Bucket)(
    testWith: TestWith[MessageReader[ExampleObject], R]) = {

    val s3Config = S3Config(bucketName = bucket.name)
    val s3ObjectStore =
      new S3ObjectStore[ExampleObject](s3Client, s3Config, keyPrefixGenerator)

    val testReader = new MessageReader[ExampleObject](s3ObjectStore)

    testWith(testReader)
  }

  def withMessageWorker[R](
    actorSystem: ActorSystem,
    metricsSender: metrics.MetricsSender,
    queue: Queue,
    bucket: S3.Bucket
  )(testWith: TestWith[ExampleMessageWorker, R]) = {

    val sqsReader = new SQSReader(sqsClient, SQSConfig(queue.url, 1.second, 1))

    val s3Config = S3Config(bucketName = bucket.name)
    val s3 =
      new S3ObjectStore[ExampleObject](s3Client, s3Config, keyPrefixGenerator)

    val messageReader = new MessageReader[ExampleObject](s3)

    val testWorker = new ExampleMessageWorker(
      sqsReader,
      messageReader,
      actorSystem,
      metricsSender
    )

    try {
      testWith(testWorker)
    } finally {
      testWorker.stop()
    }
  }

  def withMessageReaderFixtures[R] =
    withLocalS3Bucket[R] and
      withMessageReader[R] _

  def withMessageWorkerFixtures[R] =
    withActorSystem[R] and
      withMetricsSender[R] _ and
      withLocalStackSqsQueue[R] and
      withLocalS3Bucket[R] and
      withMessageWorker[R] _

  def withMessageWorkerFixturesAndMockedMetrics[R] =
    withActorSystem[R] and
      withMockMetricSender[R] and
      withLocalStackSqsQueue[R] and
      withLocalS3Bucket[R] and
      withMessageWorker[R] _

}
