package uk.ac.wellcome.test.fixtures

import akka.actor.ActorSystem
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sns.model.{
  SubscribeRequest,
  SubscribeResult,
  UnsubscribeRequest
}
import io.circe.Decoder
import io.circe._
import io.circe.generic.semiauto._
import uk.ac.wellcome.message.{MessageReader, MessageWorker}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{S3Config, SQSConfig}
import uk.ac.wellcome.s3.{KeyPrefixGenerator, S3ObjectStore}
import uk.ac.wellcome.sqs.SQSReader
import uk.ac.wellcome.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.SNS.Topic
import uk.ac.wellcome.test.fixtures.SQS.Queue

import scala.concurrent.duration._
import scala.concurrent.Future
import com.amazonaws.services.sns.model.UnsubscribeRequest

trait Messaging
    extends Akka
    with Metrics
    with SQS
    with SNS
    with S3
    with ImplicitLogging {

  def withLocalStackSubscription[R](queue: Queue, topic: Topic) =
    fixture[SubscribeResult, R](
      create = {
        val subRequest = new SubscribeRequest(topic.arn, "sqs", queue.arn)
        localStackSnsClient.subscribe(subRequest)
      },
      destroy = { subscribeResult =>
        val unsubscribeRequest =
          new UnsubscribeRequest(subscribeResult.getSubscriptionArn)
        localStackSnsClient.unsubscribe(unsubscribeRequest)
      }
    )

  case class ExampleObject(name: String)

  val keyPrefixGenerator: KeyPrefixGenerator[ExampleObject] =
    new KeyPrefixGenerator[ExampleObject] {
      override def generate(obj: ExampleObject): String = "/"
    }

  def withMessageReader[R](s3Client: AmazonS3)(bucket: Bucket)(
    testWith: TestWith[MessageReader[ExampleObject], R]) = {

    val s3Config = S3Config(bucketName = bucket.name)
    val s3ObjectStore =
      new S3ObjectStore[ExampleObject](s3Client, s3Config, keyPrefixGenerator)

    val testReader = new MessageReader[ExampleObject](s3ObjectStore)

    testWith(testReader)
  }

  def withMessageWorker[R](
    sqsClient: AmazonSQS,
    s3Client: AmazonS3
  )(actors: ActorSystem,
    queue: Queue,
    metrics: MetricsSender,
    bucket: S3.Bucket)(testWith: TestWith[MessageWorker[ExampleObject], R]) = {

    val sqsReader = new SQSReader(sqsClient, SQSConfig(queue.url, 1.second, 1))

    val s3Config = S3Config(bucketName = bucket.name)
    val s3 =
      new S3ObjectStore[ExampleObject](s3Client, s3Config, keyPrefixGenerator)

    val messageReader = new MessageReader[ExampleObject](s3)

    val testWorker =
      new MessageWorker[ExampleObject](
        sqsReader,
        messageReader,
        actors,
        metrics) {

        override implicit val decoder: Decoder[ExampleObject] =
          deriveDecoder[ExampleObject]

        override def processMessage(message: ExampleObject) =
          Future.successful(())
      }

    try {
      testWith(testWorker)
    } finally {
      testWorker.stop()
    }
  }

  def withMessageReaderFixtures[R] =
    withLocalS3Bucket[R] and
      withMessageReader[R](s3Client) _

  def withMessageWorkerFixtures[R] =
    withActorSystem[R] and
      withLocalSqsQueue[R] and
      withMetricsSender[R] _ and
      withLocalS3Bucket[R] and
      withMessageWorker[R](
        sqsClient,
        s3Client
      ) _

}
