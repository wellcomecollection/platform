package uk.ac.wellcome.platform.ingestor.modules

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.{Try, Success, Failure}

import akka.actor.{ActorSystem, Props}
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.twitter.inject.{Injector, Logging, TwitterModule}

import uk.ac.wellcome.finatra.modules.{
  AkkaModule,
  SQSClientModule,
  SQSConfigModule
}
import uk.ac.wellcome.finatra.services.ElasticsearchService
import uk.ac.wellcome.models.UnifiedItem
import uk.ac.wellcome.models.SQSConfig

import com.amazonaws.services.sqs.model.{
  DeleteMessageRequest,
  Message => AwsSQSMessage,
  ReceiveMessageRequest
}

import com.amazonaws.services.sqs.AmazonSQS

import uk.ac.wellcome.platform.ingestor.services.MessageProcessorService

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import uk.ac.wellcome.models.SQSMessage

import uk.ac.wellcome.utils.JsonUtil

object SQSWorker extends TwitterModule {
  override val modules = Seq(SQSConfigModule, SQSClientModule, AkkaModule)

  val waitTime = flag("sqs.waitTime", 20, "SQS wait time")
  val maxMessages = flag("sqs.maxMessages", 1, "Max SQS messages")

  def receiveMessageRequest(queueUrl: String)  =
    new ReceiveMessageRequest(queueUrl)
      .withWaitTimeSeconds(waitTime())
      .withMaxNumberOfMessages(maxMessages())

  def getMessages(
    client: AmazonSQS,
    receiveMessageRequest: ReceiveMessageRequest
  ): Seq[AwsSQSMessage] =
    client
      .receiveMessage(receiveMessageRequest)
      .getMessages.asScala.toList

  @tailrec
  def processMessages(
    sqsClient: AmazonSQS,
    sqsConfig: SQSConfig,
    messageProcessorService: MessageProcessorService
  ): Unit = {
    val messageRequest = receiveMessageRequest(sqsConfig.queueUrl)

    getMessages(
      sqsClient,
      messageRequest
    ).map(messageProcessorService.processMessage)

    processMessages(sqsClient, sqsConfig, messageProcessorService)
  }

  override def singletonStartup(injector: Injector) {
    info("Starting SQS worker")

    val system = injector.instance[ActorSystem]

    val sqsClient = injector.instance[AmazonSQS]
    val sqsConfig = injector.instance[SQSConfig]
    val messageProcessorService = injector.instance[MessageProcessorService]

    system.scheduler.scheduleOnce(
      Duration.create(50, TimeUnit.MILLISECONDS)
    )(processMessages(sqsClient, sqsConfig, messageProcessorService))
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    system.terminate()
  }
}
