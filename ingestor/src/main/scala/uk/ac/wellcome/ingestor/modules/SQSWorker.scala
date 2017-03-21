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

import uk.ac.wellcome.finatra.modules.AkkaModule
import uk.ac.wellcome.finatra.services.ElasticsearchService
import uk.ac.wellcome.models.UnifiedItem

import com.amazonaws.services.sqs.model.{
  DeleteMessageRequest,
  Message => SQSMessage,
  ReceiveMessageRequest
}

import com.amazonaws.services.sqs.AmazonSQS

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._

import uk.ac.wellcome.utils.JsonUtil

//TODO: Unify with transformer object
case class SNSMessage(
  Type: String,
  MessageId: String,
  TopicArn: String,
  Subject: Option[String],
  Timestamp: String,
  Message: String
) {
  def getBody(): String = Message
  def getSubject(): Option[String] = Subject
}

object SQSWorker extends TwitterModule {
  override val modules = Seq(SQSConfigModule, SQSClientModule, AkkaModule)

  def extractSNSMessage(sqsMessage: SQSMessage): Option[SNSMessage] =
    JsonUtil.fromJson[SNSMessage](sqsMessage.getBody) match {
      case Success(m) => Some(m)
      case Failure(e) => {
        error("Invalid message structure (not via SNS?)", e)
        None
      }
    }

  def getMessages(
    client: AmazonSQS,
    queueUrl: String,
    waitTime: Int,
    maxMessages: Int
  ): Seq[SQSMessage] =
    client
      .receiveMessage(
        new ReceiveMessageRequest(queueUrl)
          .withWaitTimeSeconds(waitTime)
          .withMaxNumberOfMessages(maxMessages)
      )
      .getMessages
      .asScala
      .toList

  def deleteMessage(client: AmazonSQS,
                    queueUrl: String,
                    message: SQSMessage): Unit =
    client.deleteMessage(
      new DeleteMessageRequest(queueUrl, message.getReceiptHandle))

  def chooseProcessor(subject: String)
    : Option[(String, ElasticsearchService) => Future[Unit]] = {
    PartialFunction.condOpt(subject) {
      case "example" => indexDocument
    }
  }

  //TODO: Extract processors out, everything else into a base trait
  def indexDocument(
    document: String,
    elasticsearchService: ElasticsearchService
  ): Future[Unit] = Future {
    implicit val jsonMapper = UnifiedItem

    JsonUtil
      .fromJson[UnifiedItem](document)
      .map(item => {
        elasticsearchService.client.execute {
          //TODO: Push index and type to config?
          indexInto("records" / "item").doc(item)
        }.await
      })
  }

  @tailrec
  def processMessages(
    client: AmazonSQS,
    queueUrl: String,
    elasticsearchService: ElasticsearchService
  ): Unit = {
    for (msg <- getMessages(client = client,
                            queueUrl = queueUrl,
                            waitTime = 20,
                            maxMessages = 1)) {
      val future = for {
        message <- Future(extractSNSMessage(msg).get)

        processorOption = message.getSubject().flatMap(chooseProcessor)

        processor <- Future(processorOption.getOrElse({
          error(s"Unrecognised message subject ${message.getSubject()}")
          throw new RuntimeException("Failed to process message")
        }))

        _ = processor.apply(message.getBody(), elasticsearchService)

      } yield ()

      future.onSuccess { case _ => deleteMessage(client, queueUrl, msg) }
    }

    processMessages(client, queueUrl, elasticsearchService)
  }

  override def singletonStartup(injector: Injector) {
    info("Starting SQS worker")

    val system = injector.instance[ActorSystem]

    val sqsClient = injector.instance[AmazonSQS]
    val sqsConfig = injector.instance[SQSConfig]
    val elasticsearchService = injector.instance[ElasticsearchService]

    system.scheduler.scheduleOnce(
      Duration.create(50, TimeUnit.MILLISECONDS)
    )(processMessages(sqsClient, sqsConfig.queueUrl, elasticsearchService))
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    system.terminate()
  }
}
