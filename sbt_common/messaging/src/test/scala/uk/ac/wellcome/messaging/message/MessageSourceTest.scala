package uk.ac.wellcome.messaging.message

import akka.Done
import akka.stream.KillSwitch
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sqs.AmazonSQS
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.storage.s3.S3Config
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}

import scala.concurrent.Future
import scala.concurrent.duration._

class MessageSourceTest extends FunSpec with Matchers with Messaging with Akka with ScalaFutures{

  it("reads messages off a queue, processes them and deletes them") {
    withActorSystem {actorSystem =>
      withMaterializer(actorSystem) { materialiser =>
        withMessageStreamFixtures { case (bucket, messageStream, queue) =>
          sqsClient.sendMessage(queue.url, )

          val future = messageStream.foreach((o: ExampleObject) => Future.successful())

            whenReady(future){ _ =>

            }
        }
      }
    }
  }

  def withMessageStreamFixtures[R](testWith: TestWith[(Bucket, MessageStream, Queue), R]) = {
    withLocalS3Bucket { bucket =>
      withLocalStackSqsQueue { queue =>
        val s3Config = S3Config(bucketName = bucket.name)
        val sqsConfig = SQSConfig(queueUrl = queue.url, waitTime = 1 millisecond, maxMessages = 1)

        val messageConfig = MessageReaderConfig(
          sqsConfig = sqsConfig,
          s3Config = s3Config
        )

        val stream = new MessageStream(sqsClient, s3Client, messageConfig)
        testWith((bucket, stream, queue))
      }
    }
  }

}

class MessageStream(sqsClient: AmazonSQS, s3Client: AmazonS3, messageReaderConfig: MessageReaderConfig) {
  def foreach[T](objectToEventualUnit: T => Future[Unit]): Future[Done] = ???

}
