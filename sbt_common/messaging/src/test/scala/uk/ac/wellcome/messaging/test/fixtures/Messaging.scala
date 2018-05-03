package uk.ac.wellcome.messaging.test.fixtures

import akka.actor.ActorSystem
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.{SubscribeRequest, SubscribeResult, UnsubscribeRequest}
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import org.scalatest.Matchers
import uk.ac.wellcome.messaging.message.{MessageConfig, MessageReader, MessageWorker, MessageWriter, _}
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig}
import uk.ac.wellcome.messaging.sqs.{SQSConfig, SQSReader}
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.storage.s3.{KeyPrefixGenerator, S3Config, S3ObjectLocation}
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.utils.JsonUtil._

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success
import uk.ac.wellcome.monitoring
import uk.ac.wellcome.monitoring.test.fixtures.{
  MetricsSender => MetricsSenderFixture
}

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
    messageReaderLocalFlags(bucket, queue) ++ messageWriterLocalFlags(bucket, topic)

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
                              metricsSender: monitoring.MetricsSender
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
    withMessageReader(bucket, queue, keyPrefixGenerator)(testWith)
  }

  def withMessageReader[T, R](bucket: Bucket, queue: Queue, keyPrefixGenerator: KeyPrefixGenerator[T])(
    testWith: TestWith[MessageReader[T], R]) = {

    val s3Config = S3Config(bucketName = bucket.name)
    val sqsConfig = SQSConfig(queueUrl = queue.url, waitTime = 1 millisecond, maxMessages = 1)

    val messageConfig = MessageReaderConfig(
      sqsConfig = sqsConfig,
      s3Config = s3Config
    )

    val testReader = new MessageReader[T](
      messageReaderConfig = messageConfig,
      s3Client = s3Client,
      keyPrefixGenerator = keyPrefixGenerator,
      sqsClient = sqsClient
    )

    testWith(testReader)
  }

  def withMessageWorker[R](
                            actorSystem: ActorSystem,
                            metricsSender: monitoring.MetricsSender,
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
          withExampleObjectMessageReader(bucket = bucket, queue = queue) { reader =>
            testWith((bucket, reader, queue))
          }
        }
      }
  }

  def withExampleObjectMessageWorkerFixtures[R](
                                                 testWith: TestWith[(monitoring.MetricsSender,
                                                   Queue,
                                                   Bucket,
                                                   ExampleMessageWorker),
                                                   R]) = {
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withExampleObjectMessageReaderFixtures {
          case (bucket, messageReader, queue) =>
            withMessageWorker(
              actorSystem,
              metricsSender,
              queue,
              messageReader) { worker =>
              testWith((metricsSender, queue, bucket, worker))
            }
        }

      }
    }
  }

  def withMessageWorkerFixturesAndMockedMetrics[R](
                                                    testWith: TestWith[(monitoring.MetricsSender,
                                                      Queue,
                                                      Bucket,
                                                      ExampleMessageWorker),
                                                      R]) = {
    withActorSystem { actorSystem =>
      withMockMetricSender { metricsSender =>
        withExampleObjectMessageReaderFixtures {
          case (bucket, messageReader, queue) =>
            withMessageWorker(
              actorSystem,
              metricsSender,
              queue,
              messageReader) { worker =>
              testWith((metricsSender, queue, bucket, worker))
            }
        }

      }
    }
  }

  def assertQueueEmpty(queue: Queue) = {
    Thread.sleep(1500)

    val messages = sqsClient
      .receiveMessage(
        new ReceiveMessageRequest(queue.url)
          .withMaxNumberOfMessages(1)
      )
      .getMessages
      .toList

    messages shouldBe empty
  }

  def assertQueueNotEmpty(queue: Queue) = {
    Thread.sleep(1500)

    val messages = sqsClient
      .receiveMessage(
        new ReceiveMessageRequest(queue.url)
          .withMaxNumberOfMessages(1)
      )
      .getMessages
      .toList

    messages should not be empty
  }

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
}
