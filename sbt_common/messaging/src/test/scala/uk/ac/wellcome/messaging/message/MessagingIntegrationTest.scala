package uk.ac.wellcome.messaging.message

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

class MessagingIntegrationTest
    extends FunSpec
    with Matchers
    with Messaging
    with Eventually
    with ExtendedPatience {

  val message = ExampleObject("A message sent in the MessagingIntegrationTest")
  val subject = "message-integration-test-subject"

  it("sends and receives messages") {
    withLocalStackMessageWriter {
      case (worker, messageWriter) =>
        messageWriter.write(message = message, subject = subject)

        eventually {
          worker.calledWith shouldBe Some(message)
        }
    }
  }

  private def withLocalStackMessageWriter[R](
    testWith: TestWith[(ExampleMessageWorker, MessageWriter[ExampleObject]),
                       R]) = {
    withExampleObjectMessageWorkerFixtures {
      case (metricsSender, queue, bucket, worker) =>
        withLocalStackSnsTopic { topic =>
          withLocalStackSubscription(queue, topic) { _ =>
            withMessageWriter(bucket, topic, localStackSnsClient) {
              messageWriter =>
                testWith((worker, messageWriter))
            }
          }
        }
    }
  }
}
