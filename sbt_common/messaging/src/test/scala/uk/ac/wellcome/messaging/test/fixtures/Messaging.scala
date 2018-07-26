package uk.ac.wellcome.messaging.test.fixtures

import akka.actor.ActorSystem
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.{
  SubscribeRequest,
  SubscribeResult,
  UnsubscribeRequest
}
import com.amazonaws.services.sqs.model.SendMessageResult
import io.circe.{Decoder, Encoder}
import org.scalatest.Matchers
import uk.ac.wellcome.messaging.message._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.{Queue, QueuePair}
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.storage.{ObjectLocation, ObjectStore}
import uk.ac.wellcome.storage.s3.S3Config
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures._

import scala.concurrent.duration._
import scala.util.{Random, Success}
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

trait Messaging
    extends Akka
    with MetricsSenderFixture
    with SQS
    with SNS
    with S3
    with Matchers {

  case class ExampleObject(name: String)

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

  def withExampleObjectMessageWriter[R](bucket: Bucket,
                                        topic: Topic,
                                        writerSnsClient: AmazonSNS = snsClient)(
    testWith: TestWith[MessageWriter[ExampleObject], R]) = {
    withMessageWriter[ExampleObject, R](bucket, topic, writerSnsClient)(
      testWith)
  }

  def withMessageWriter[T, R](bucket: Bucket,
                              topic: Topic,
                              writerSnsClient: AmazonSNS = snsClient)(
    testWith: TestWith[MessageWriter[T], R])(implicit store: ObjectStore[T]) = {
    val s3Config = S3Config(bucketName = bucket.name)
    val snsConfig = SNSConfig(topicArn = topic.arn)
    val messageConfig = MessageWriterConfig(
      s3Config = s3Config,
      snsConfig = snsConfig
    )

    val messageWriter = new MessageWriter[T](
      messageConfig = messageConfig,
      snsClient = writerSnsClient,
      s3Client = s3Client
    )

    testWith(messageWriter)
  }

  def withMessageStream[T, R](
    actorSystem: ActorSystem,
    bucket: Bucket,
    queue: SQS.Queue,
    metricsSender: MetricsSender)(testWith: TestWith[MessageStream[T], R])(
    implicit objectStore: ObjectStore[T]) = {
    val s3Config = S3Config(bucketName = bucket.name)
    val sqsConfig =
      SQSConfig(queueUrl = queue.url, waitTime = 1 millisecond, maxMessages = 1)

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

  def withMessageStreamFixtures[T, R](
    testWith: TestWith[(Bucket, MessageStream[T], QueuePair, MetricsSender), R]
  )(implicit objectStore: ObjectStore[T]) = {

    withActorSystem { actorSystem =>
      withLocalS3Bucket { bucket =>
        withLocalSqsQueueAndDlq {
          case queuePair @ QueuePair(queue, _) =>
            withMockMetricSender { metricsSender =>
              withMessageStream[T, R](actorSystem, bucket, queue, metricsSender) {
                stream =>
                  testWith((bucket, stream, queuePair, metricsSender))
              }

            }
        }
      }
    }
  }

  private def put[T](obj: T, location: ObjectLocation)(
    implicit encoder: Encoder[T]) = {
    val serialisedObj = toJson[T](obj).get

    s3Client.putObject(
      location.namespace,
      location.key,
      serialisedObj
    )

    val examplePointer =
      MessagePointer(ObjectLocation(location.namespace, location.key))

    val exampleNotification = createNotificationMessageWith(
      message = examplePointer
    )

    toJson(exampleNotification).get
  }

  private def get[T](snsMessage: MessageInfo)(
    implicit decoder: Decoder[T]): T = {
    val tryMessagePointer = fromJson[MessagePointer](snsMessage.message)
    tryMessagePointer shouldBe a[Success[_]]

    val messagePointer = tryMessagePointer.get

    getObjectFromS3[T](
      bucket = Bucket(messagePointer.src.namespace),
      key = messagePointer.src.key
    )
  }

  /** Given a topic ARN which has received notifications containing pointers
    * to objects in S3, return the unpacked objects.
    */
  def getMessages[T](topic: Topic)(implicit decoder: Decoder[T]): List[T] =
    listMessagesReceivedFromSNS(topic).map { snsMessage =>
      get[T](snsMessage)
    }.toList

  /** Store an object in S3 and send the notification to SQS.
    *
    * As if another application had used a MessageWriter to send the message
    * to an SNS topic, which was forwarded to the queue.  We don't use a
    * MessageWriter instance because that sends to SNS, not SQS.
    */
  def sendMessage[T](bucket: Bucket, queue: Queue, obj: T)(
    implicit encoder: Encoder[T]): SendMessageResult = {
    val s3key = Random.alphanumeric take 10 mkString
    val notificationJson = put[T](
      obj = obj,
      location = ObjectLocation(namespace = bucket.name, key = s3key)
    )

    sendMessage(queue = queue, body = notificationJson)
  }
}
