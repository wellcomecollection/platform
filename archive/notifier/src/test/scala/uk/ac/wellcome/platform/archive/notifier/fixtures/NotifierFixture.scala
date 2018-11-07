package uk.ac.wellcome.platform.archive.notifier.fixtures

import java.net.{URI, URL}

import com.amazonaws.services.sns.model.PublishResult
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.{Queue, QueuePair}
import uk.ac.wellcome.platform.archive.notifier.Notifier
import uk.ac.wellcome.platform.archive.common.fixtures.BagIt
import uk.ac.wellcome.platform.archive.common.messaging.MessageStream
import uk.ac.wellcome.platform.archive.common.models.NotificationMessage
import uk.ac.wellcome.platform.archive.common.progress.models.Namespace
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.test.fixtures.TestWith

trait NotifierFixture extends S3 with Messaging with BagIt {

  protected val callbackHost = "localhost"
  protected val callbackPort = 8080

  val space = Namespace("space-id")
  val uploadUri = new URI(s"http://www.example.com/asset")

  def withApp[R](queue: Queue, topic: Topic)(
    testWith: TestWith[Notifier, R]): R =
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { materializer =>
        withMetricsSender(actorSystem) { metricsSender =>
          val messageStream = new MessageStream[NotificationMessage, PublishResult](
            actorSystem = actorSystem,
            sqsClient = asyncSqsClient,
            sqsConfig = createSQSConfigWith(queue),
            metricsSender = metricsSender
          )

          val notifier = new Notifier(
            messageStream = messageStream,
            snsClient = snsClient,
            snsConfig = createSNSConfigWith(topic),
            contextUrl = new URL("http://localhost/context.json")
          )(
            actorSystem = actorSystem,
            materializer = materializer
          )

          testWith(notifier)
        }
      }
    }

  def withNotifier[R](
    testWith: TestWith[(QueuePair, Topic, Notifier), R]): R = {
    withLocalSqsQueueAndDlqAndTimeout(15)(queuePair => {
      withLocalSnsTopic { topic =>
        withApp(queue = queuePair.queue, topic = topic) { app =>
          testWith((queuePair, topic, app))
        }
      }
    })
  }
}
