package uk.ac.wellcome.platform.ingestor.modules

import scala.collection.JavaConverters._
import scala.concurrent.Future
import uk.ac.wellcome.utils.TryBackoff
import akka.actor.{ActorSystem}
import com.twitter.inject.{Injector, Logging, TwitterModule}

import uk.ac.wellcome.models.aws.SQSConfig
import uk.ac.wellcome.finatra.modules.{
  AkkaModule,
  SQSClientModule,
  SQSConfigModule
}

import com.amazonaws.services.sqs.AmazonSQS

import uk.ac.wellcome.platform.ingestor.services.MessageProcessorService

import com.amazonaws.services.sqs.model.{
  Message => AwsSQSMessage,
  ReceiveMessageRequest
}

object SQSWorker extends TwitterModule with TryBackoff {
  override val modules = Seq(SQSConfigModule, SQSClientModule, AkkaModule)

  val waitTime = flag("sqs.waitTime", 20, "SQS wait time")
  val maxMessages = flag("sqs.maxMessages", 1, "Max SQS messages")

  def receiveMessageRequest(queueUrl: String) =
    new ReceiveMessageRequest(queueUrl)
      .withWaitTimeSeconds(waitTime())
      .withMaxNumberOfMessages(maxMessages())

  def getMessages(
    client: AmazonSQS,
    receiveMessageRequest: ReceiveMessageRequest
  ): Seq[AwsSQSMessage] =
    client
      .receiveMessage(receiveMessageRequest)
      .getMessages
      .asScala
      .toList

  def processMessages(
    sqsClient: AmazonSQS,
    sqsConfig: SQSConfig,
    messageProcessorService: MessageProcessorService
  ): Unit = {
    info("Polling for new messages ...")
    val messageRequest = receiveMessageRequest(sqsConfig.queueUrl)

    getMessages(
      sqsClient,
      messageRequest
    ).map(messageProcessorService.processMessage)

  }

  override def singletonStartup(injector: Injector) {
    info("Starting SQS worker")

    val system = injector.instance[ActorSystem]

    val sqsClient = injector.instance[AmazonSQS]
    val sqsConfig = injector.instance[SQSConfig]
    val messageProcessorService = injector.instance[MessageProcessorService]

    def start() =
      processMessages(sqsClient, sqsConfig, messageProcessorService)

    run(start, system)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    system.terminate()
  }
}
