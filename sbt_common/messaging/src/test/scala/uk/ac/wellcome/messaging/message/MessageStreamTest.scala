package uk.ac.wellcome.messaging.message

import akka.Done
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.stream.alpakka.sqs.MessageAction
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSource}
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sqs
import com.amazonaws.services.sqs.AmazonSQSAsync
import io.circe.Decoder
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SQS.{Queue, QueuePair}
import uk.ac.wellcome.storage.s3.{KeyPrefixGenerator, S3Config, S3ObjectLocation, S3ObjectStore}
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class MessageStreamTest extends FunSpec with Matchers with Messaging with Akka with ScalaFutures with ExtendedPatience {

  it("reads messages off a queue, processes them and deletes them") {

    withMessageStreamFixtures { case (bucket, messageStream, QueuePair(queue, dlq)) =>
      val key = "message-key"
      val exampleObject = ExampleObject("some value")

      val notice = put(exampleObject, S3ObjectLocation(bucket.name, key))

      sqsClient.sendMessage(
        queue.url,
        notice
      )

      var received: List[ExampleObject] = Nil
      val f = (o: ExampleObject) => {

        synchronized {
          received = o :: received
        }

        Future.successful(())
      }
      messageStream.foreach(f)

      eventually {
        received shouldBe List(exampleObject)

        assertQueueEmpty(queue)
      }
    }


  }

  it("fails gracefully when NotificationMessage cannot be deserialised") {
    withMessageStreamFixtures {
      case (bucket, messageStream, QueuePair(queue, dlq)) =>
        sqsClient.sendMessage(
          queue.url,
          "not valid json"
        )

        var received: List[ExampleObject] = Nil
        val f = (o: ExampleObject) => {

          synchronized {
            received = o :: received
          }

          Future.successful(())
        }

        messageStream.foreach(f)

        eventually {
          received shouldBe Nil

          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, 1)
        }
    }
  }

  it("does not fails gracefully when the s3 object cannot be retrieved") {
    withMessageStreamFixtures {
      case (bucket, messageStream, QueuePair(queue, dlq)) =>
        val key = "key.json"

        // Do NOT put S3 object here

        val examplePointer =
          MessagePointer(S3ObjectLocation(bucket.name, key))
        val serialisedExamplePointer = toJson(examplePointer).get

        val exampleNotification = NotificationMessage(
          MessageId = "MessageId",
          TopicArn = "TopicArn",
          Subject = "Subject",
          Message = serialisedExamplePointer
        )

        val serialisedExampleNotification = toJson(exampleNotification).get

        sqsClient.sendMessage(
          queue.url,
          serialisedExampleNotification
        )

        var received: List[ExampleObject] = Nil
        val f = (o: ExampleObject) => {

          synchronized {
            received = o :: received
          }

          Future.successful(())
        }

        messageStream.foreach(f)

        eventually {
          // TODO what do graceful and non graceful failure means in this case?
          //          throwable shouldBe a[AmazonS3Exception]

          received shouldBe Nil

          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, 1)
        }
    }
  }

  it("continues reading if processing of some messages fails ") {
    withMessageStreamFixtures { case (bucket, messageStream, QueuePair(queue, dlq)) =>
      val key = "message-key"
      val exampleObject = ExampleObject("some value")

      sqsClient.sendMessage(
        queue.url,
        "not valid json"
      )

      val firstNotice = put(exampleObject, S3ObjectLocation(bucket.name, key))

      sqsClient.sendMessage(
        queue.url,
        firstNotice
      )

      sqsClient.sendMessage(
        queue.url,
        "another not valid json"
      )

      val secondNotice = put(exampleObject, S3ObjectLocation(bucket.name, key))

      sqsClient.sendMessage(
        queue.url,
        secondNotice
      )

      var received: List[ExampleObject] = Nil
      val f = (o: ExampleObject) => {

        synchronized {
          received = o :: received
        }

        Future.successful(())
      }
      messageStream.foreach(f)

      eventually {
        received shouldBe List(exampleObject, exampleObject)

        assertQueueEmpty(queue)
        assertQueueHasSize(dlq, 2)
      }
    }
  }

  def withMessageStreamFixtures[R](testWith: TestWith[(Bucket, MessageStream[ExampleObject], QueuePair), R]) = {
    withActorSystem { actorSystem =>
      withLocalS3Bucket { bucket =>
        withLocalSqsQueueAndDlq { case queuePair @ QueuePair(queue, dlq) =>
          val s3Config = S3Config(bucketName = bucket.name)
          val sqsConfig = SQSConfig(queueUrl = queue.url, waitTime = 1 millisecond, maxMessages = 1)

          val messageConfig = MessageReaderConfig(
            sqsConfig = sqsConfig,
            s3Config = s3Config
          )

          val stream = new MessageStream[ExampleObject](actorSystem, asyncSqsClient, s3Client, messageConfig)
          testWith((bucket, stream, queuePair))
        }
      }
    }
  }

  class MessageStream[T](actorSystem: ActorSystem, sqsClient: AmazonSQSAsync, s3Client: AmazonS3, messageReaderConfig: MessageReaderConfig) {
    implicit val system = actorSystem
    val decider: Supervision.Decider = {
      case _: Exception => Supervision.Resume
      case _            => Supervision.Stop
    }
    implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system).withSupervisionStrategy(decider))

    val s3ObjectStore = new S3ObjectStore[T](
      s3Client = s3Client,
      s3Config = messageReaderConfig.s3Config
    )

    def foreach(f: T => Future[Unit])(implicit decoderT: Decoder[T]): Future[Done] = SqsSource(messageReaderConfig.sqsConfig.queueUrl)(sqsClient)
      .mapAsyncUnordered(10) {
        message =>
          for {
            t <- read(message)
            _ <- f(t)
          } yield message
      }
      .map { m =>
        (m, MessageAction.Delete)
      }
      .runWith(SqsAckSink(messageReaderConfig.sqsConfig.queueUrl)(sqsClient))

    private def read(message: sqs.model.Message)(
      implicit decoderN: Decoder[NotificationMessage],
      decoderT: Decoder[T]
    ): Future[T] = {
      val deserialisedMessagePointerAttempt = for {
        notification <- fromJson[NotificationMessage](message.getBody)
        deserialisedMessagePointer <- fromJson[MessagePointer](
          notification.Message)
      } yield deserialisedMessagePointer

      for {
        messagePointer <- Future.fromTry[MessagePointer](
          deserialisedMessagePointerAttempt)
        deserialisedObject <- s3ObjectStore.get(messagePointer.src)
      } yield deserialisedObject
    }

  }

}
