package uk.ac.wellcome.messaging.test.fixtures

import akka.actor.ActorSystem
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.UnsubscribeRequest
import com.amazonaws.services.sns.model.{SubscribeRequest, SubscribeResult}
import org.scalatest.Matchers
import uk.ac.wellcome.messaging.message._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig}
import uk.ac.wellcome.messaging.sqs.{SQSConfig, SQSReader}
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.storage.s3.{
  KeyPrefixGenerator,
  S3Config,
  S3ObjectLocation
}
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Random, Success}

trait Messaging
    extends Akka
    with MetricsSenderFixture
    with SQS
    with SNS
    with S3
    with Matchers {

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

  def messagingLocalFlags(bucket: Bucket, topic: Topic, queue: Queue) =
    messageReaderLocalFlags(bucket, queue) ++ messageWriterLocalFlags(
      bucket,
      topic)

  def messageReaderLocalFlags(bucket: Bucket, queue: Queue) =
    Map(
      "aws.message.s3.bucketName" -> bucket.name,
      "aws.message.sqs.queue.url" -> queue.url
    ) ++ s3ClientLocalFlags ++ sqsLocalClientFlags

  def messageWriterLocalFlags(bucket: Bucket, topic: Topic) =
    Map(
      "aws.message.sns.topic.arn" -> topic.arn,
      "aws.message.s3.bucketName" -> bucket.name
    ) ++ s3ClientLocalFlags ++ snsLocalClientFlags

  case class ExampleObject(name: String)

  class ExampleMessageWorker(
    messageReader: MessageReader[ExampleObject],
    actorSystem: ActorSystem,
    metricsSender: MetricsSender
  ) extends MessageWorker[ExampleObject](
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

  def withExampleObjectMessageReader[R](bucket: Bucket, queue: Queue)(
    testWith: TestWith[MessageReader[ExampleObject], R]) = {
    withMessageReader(bucket, queue)(testWith)
  }

  def withMessageReader[T, R](bucket: Bucket, queue: Queue)(
    testWith: TestWith[MessageReader[T], R]) = {

    val s3Config = S3Config(bucketName = bucket.name)
    val sqsConfig = SQSConfig(
      queueUrl = queue.url,
      waitTime = 1 millisecond,
      maxMessages = 1)

    val messageReaderConfig = MessageReaderConfig(
      sqsConfig = sqsConfig,
      s3Config = s3Config
    )

    val testReader = new MessageReader[T](
      messageReaderConfig = messageReaderConfig,
      s3Client = s3Client,
      sqsClient = sqsClient
    )

    testWith(testReader)
  }

  def withMessageWorker[R](
    actorSystem: ActorSystem,
    metricsSender: MetricsSender,
    queue: Queue,
    messageReader: MessageReader[ExampleObject]
  )(testWith: TestWith[ExampleMessageWorker, R]) = {

    val sqsReader = new SQSReader(sqsClient, SQSConfig(queue.url, 1.second, 1))

    val testWorker = new ExampleMessageWorker(
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

  def withMessageWriter[R](bucket: Bucket,
                           topic: Topic,
                           writerSnsClient: AmazonSNS = snsClient)(
    testWith: TestWith[MessageWriter[ExampleObject], R]) = {
    val s3Config = S3Config(bucketName = bucket.name)
    val snsConfig = SNSConfig(topicArn = topic.arn)
    val messageConfig = MessageWriterConfig(
      s3Config = s3Config,
      snsConfig = snsConfig
    )

    val messageWriter = new MessageWriter[ExampleObject](
      messageConfig = messageConfig,
      snsClient = writerSnsClient,
      s3Client = s3Client,
      keyPrefixGenerator = keyPrefixGenerator
    )

    testWith(messageWriter)
  }

  def withExampleObjectMessageReaderFixtures[R](
    testWith: TestWith[(Bucket, MessageReader[ExampleObject], Queue), R]) = {
    withLocalS3Bucket { bucket =>
      withLocalStackSqsQueue { queue =>
        withExampleObjectMessageReader(bucket = bucket, queue = queue) {
          reader =>
            testWith((bucket, reader, queue))
        }
      }
    }
  }

  def withExampleObjectMessageWorkerFixtures[R](
    testWith: TestWith[(MetricsSender, Queue, Bucket, ExampleMessageWorker),
                       R]) = {
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withExampleObjectMessageReaderFixtures {
          case (bucket, messageReader, queue) =>
            withMessageWorker(actorSystem, metricsSender, queue, messageReader) {
              worker =>
                testWith((metricsSender, queue, bucket, worker))
            }
        }

      }
    }
  }

  def withMessageWorkerFixturesAndMockedMetrics[R](
    testWith: TestWith[(MetricsSender, Queue, Bucket, ExampleMessageWorker),
                       R]) = {
    withActorSystem { actorSystem =>
      withMockMetricSender { metricsSender =>
        withExampleObjectMessageReaderFixtures {
          case (bucket, messageReader, queue) =>
            withMessageWorker(actorSystem, metricsSender, queue, messageReader) {
              worker =>
                testWith((metricsSender, queue, bucket, worker))
            }
        }

      }
    }
  }

  def withMessageStream[T,R](actorSystem: ActorSystem, bucket: Bucket, queue: SQS.Queue, metricsSender: MetricsSender)(testWith: TestWith[MessageStream[T], R]) = {
    val s3Config = S3Config(bucketName = bucket.name)
    val sqsConfig = SQSConfig(
      queueUrl = queue.url,
      waitTime = 1 millisecond,
      maxMessages = 1)

    val messageConfig = MessageReaderConfig(
      sqsConfig = sqsConfig,
      s3Config = s3Config
    )

    val stream = new MessageStream[T](
      actorSystem,
      asyncSqsClient,
      s3Client,
      messageConfig,
      metricsSender)
    testWith(stream)
  }

  implicit val messagePointerDecoder: Decoder[MessagePointer] =
    deriveDecoder[MessagePointer]

  implicit val messagePointerEncoder: Encoder[MessagePointer] =
    deriveEncoder[MessagePointer]

  def put[T](obj: T, location: S3ObjectLocation)(
    implicit encoder: Encoder[T]) = {
    val serialisedExampleObject = toJson[T](obj).get

    s3Client.putObject(
      location.bucket,
      location.key,
      serialisedExampleObject
    )

    val examplePointer =
      MessagePointer(S3ObjectLocation(location.bucket, location.key))

    val serialisedExamplePointer = toJson(examplePointer).get

    val exampleNotification = NotificationMessage(
      MessageId = "MessageId",
      TopicArn = "TopicArn",
      Subject = "Subject",
      Message = serialisedExamplePointer
    )

    toJson(exampleNotification).get
  }

  def get[T](snsMessage: MessageInfo)(implicit decoder: Decoder[T]): T = {
    val tryMessagePointer = fromJson[MessagePointer](snsMessage.message)
    tryMessagePointer shouldBe a[Success[_]]

    val messagePointer = tryMessagePointer.get

    val tryT = fromJson[T](
      getContentFromS3(
        Bucket(messagePointer.src.bucket),
        messagePointer.src.key))
    tryT shouldBe a[Success[_]]

    tryT.get
  }

  def sendMessage(bucket: Bucket,
                  queue: SQS.Queue,
                  exampleObject: ExampleObject) = {
    val key = Random.alphanumeric take 10 mkString
    val notice = put(exampleObject, S3ObjectLocation(bucket.name, key))

    sqsClient.sendMessage(
      queue.url,
      notice
    )
  }
}
