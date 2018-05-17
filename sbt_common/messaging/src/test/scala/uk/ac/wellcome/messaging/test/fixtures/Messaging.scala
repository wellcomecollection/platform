package uk.ac.wellcome.messaging.test.fixtures

import akka.actor.ActorSystem
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.{SubscribeRequest, SubscribeResult, UnsubscribeRequest}
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import org.scalatest.Matchers
import uk.ac.wellcome.messaging.message._
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig}
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.{Queue, QueuePair}
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.storage.s3._
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSWriter

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
      "aws.message.reader.s3.bucketName" -> bucket.name,
      "aws.message.reader.sqs.queue.url" -> queue.url,
      "aws.message.reader.sqs.waitTime" -> "1",
    ) ++ s3ClientLocalFlags ++ sqsLocalClientFlags

  def messageWriterLocalFlags(bucket: Bucket, topic: Topic) =
    Map(
      "aws.message.writer.sns.topic.arn" -> topic.arn,
      "aws.message.writer.s3.bucketName" -> bucket.name
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
    testWith: TestWith[MessageReader[T], R])(implicit encoder: Encoder[T], decoder: Decoder[T]) = {

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
    testWith: TestWith[MessageWriter[ExampleObject, S3TypeMessageSender[ExampleObject]], R]) = {

    val s3Config = S3Config(bucketName = bucket.name)
    val snsConfig = SNSConfig(topicArn = topic.arn)
    val snsWriter = new SNSWriter(snsClient, snsConfig)
    val s3TypeStore = new S3TypeStore[ExampleObject](s3Client)

    val messageSender = new S3TypeMessageSender[ExampleObject](snsWriter, s3TypeStore)

    val messageWriter = new MessageWriter[ExampleObject, S3TypeMessageSender[ExampleObject]](
      snsWriter = snsWriter,
      messageSender = messageSender
    )

    testWith(messageWriter)
  }

  def withSNSWriter[R](snsClient: AmazonSNS, topic: Topic)(testWith: TestWith[SNSWriter, R]): R = {
    val snsConfig = SNSConfig(topicArn = topic.arn)
    val snsWriter = new SNSWriter(snsClient, snsConfig)

    testWith(snsWriter)
  }

  def withS3TypeStore[T,R](s3Client: AmazonS3, s3Config: S3Config)(testWith: TestWith[S3TypeStore[T], R]) = {
    val s3StringStore = new S3StringStore(s3Client, s3Config)
    val s3TypeStore = new S3TypeStore[T](s3StringStore)

    testWith(s3TypeStore)
  }

  def withS3TypeMessageRetriever[T, R](s3TypeStore: S3TypeStore[T])(testWith: TestWith[S3TypeMessageRetriever[T], R]) = {
    val messageRetriever = new S3TypeMessageRetriever[T](s3TypeStore)

    testWith(messageRetriever)
  }

  def withS3TypeMessageSender[T, R](snsWriter: SNSWriter, s3TypeStore: S3TypeStore[T])(testWith: TestWith[S3TypeMessageSender[T], R]) = {
    val messageSender = new S3TypeMessageSender[T](snsWriter, s3TypeStore)

    testWith(messageSender)
  }

  def withTypeMessageRetriever[T, R]()(testWith: TestWith[TypeMessageRetriever[T], R]) = {
    val messageRetriever = new TypeMessageRetriever[T]()

    testWith(messageRetriever)
  }

  def withTypeMessageSender[T, R](snsWriter: SNSWriter)(testWith: TestWith[TypeMessageSender[T], R]) = {
    val messageSender = new TypeMessageSender[T](snsWriter)

    testWith(messageSender)
  }

  def withMessageStreamFixtures[R](
                                    testWith: TestWith[(Bucket,
                                      MessageStream[ExampleObject],
                                      QueuePair,
                                      MetricsSender),
                                      R]) = {

    withActorSystem { actorSystem =>
      withLocalS3Bucket { bucket =>
        withLocalSqsQueueAndDlq {
          case queuePair @ QueuePair(queue, _) =>
            withMockMetricSender { metricsSender =>
              withMessageStream[ExampleObject, R](
                actorSystem,
                bucket,
                queue,
                metricsSender) { stream =>
                testWith((bucket, stream, queuePair, metricsSender))
              }

            }
        }
      }
    }
  }

  def withExampleObjectMessageReaderFixtures[R](
    testWith: TestWith[(Bucket, MessageReader[ExampleObject], Queue), R]) = {
    withLocalS3Bucket { bucket =>
      withLocalSqsQueue { queue =>
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

  def withMessageStream[T, R](
    actorSystem: ActorSystem,
    bucket: Bucket,
    queue: SQS.Queue,
    metricsSender: MetricsSender)(testWith: TestWith[MessageStream[T], R])(implicit encoder: Encoder[T], decoder: Decoder[T]) = {
    val s3Config = S3Config(bucketName = bucket.name)
    val sqsConfig = SQSConfig(
      queueUrl = queue.url,
      waitTime = 1 millisecond,
      maxMessages = 1)

    val s3TypeStore = new S3TypeStore[T](s3Client)
    val messageRetriever = new S3TypeMessageRetriever[T](s3TypeStore)

    val messageReaderConfig = MessageReaderConfig(sqsConfig, s3Config)

    val stream = new MessageStream[T](
      actorSystem,
      asyncSqsClient,
      s3Client,
      messageReaderConfig,
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
