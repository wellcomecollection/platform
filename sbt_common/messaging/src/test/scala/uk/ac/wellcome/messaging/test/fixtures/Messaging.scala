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
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.{Queue, QueuePair}
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures._

import scala.util.Success
import uk.ac.wellcome.json.JsonUtil._

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
    testWith: TestWith[MessageWriter[T], R])(
    implicit store: ObjectStore[T]): R = {
    val messageConfig = MessageWriterConfig(
      s3Config = createS3ConfigWith(bucket),
      snsConfig = createSNSConfigWith(topic)
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
    val messageConfig = MessageReaderConfig(
      sqsConfig = createSQSConfigWith(queue)
    )

    val stream = new MessageStream[T](
      actorSystem,
      asyncSqsClient,
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

  /** Given a topic ARN which has received notifications containing pointers
    * to objects in S3, return the unpacked objects.
    */
  def getMessages[T](topic: Topic)(implicit decoder: Decoder[T]): List[T] =
    listMessagesReceivedFromSNS(topic).map { messageInfo =>
      fromJson[MessageNotification](messageInfo.message) match {
        case Success(RemoteNotification(location)) =>
          getObjectFromS3[T](
            bucket = Bucket(location.namespace),
            key = location.key
          )
        case Success(InlineNotification(jsonString)) =>
          fromJson[T](jsonString).get
        case _ =>
          throw new RuntimeException(
            s"Unrecognised message: ${messageInfo.message}"
          )
      }
    }.toList

  /** Send a MessageNotification to SQS.
    *
    * As if another application had used a MessageWriter to send the message
    * to an SNS topic, which was forwarded to the queue.  We don't use a
    * MessageWriter instance because that sends to SNS, not SQS.
    *
    * We always send an InlineNotification regardless of size, which makes for
    * slightly easier debugging if queue messages ever fail.
    *
    */
  def sendMessage[T](queue: Queue, obj: T)(implicit encoder: Encoder[T]): SendMessageResult =
    sendNotificationToSQS[MessageNotification](
      queue = queue,
      message = InlineNotification(jsonString = toJson(obj).get)
    )
}
