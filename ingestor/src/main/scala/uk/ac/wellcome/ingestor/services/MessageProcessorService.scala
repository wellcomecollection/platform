package uk.ac.wellcome.platform.ingestor.services

import javax.inject.{Inject, Singleton}
import uk.ac.wellcome.finatra.services.ElasticsearchService
import uk.ac.wellcome.models.aws.MessageProcessor
import scala.concurrent.Future
import uk.ac.wellcome.models.UnifiedItem
import uk.ac.wellcome.utils.JsonUtil

import com.twitter.inject.Logging

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._

import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.models.aws.SQSConfig
import com.amazonaws.services.sqs.AmazonSQS
import scala.concurrent.ExecutionContext.Implicits.global


@Singleton
class MessageProcessorService @Inject()(
  @Flag("es.index") esIndex: String,
  @Flag("es.type") esType: String,
  elasticsearchService: ElasticsearchService,
  sqsConfig: SQSConfig,
  sqsClient: AmazonSQS
) extends MessageProcessor {

  val client = sqsClient
  val queueUrl = sqsConfig.queueUrl

  def chooseProcessor(subject: String)
    : Option[(String) => Future[Unit]] = {
    PartialFunction.condOpt(subject) {
      case "example" => indexDocument
    }
  }

  def indexDocument(document: String): Future[Unit] = Future {
    implicit val jsonMapper = UnifiedItem

    JsonUtil
      .fromJson[UnifiedItem](document)
      .map(item => {
        elasticsearchService.client.execute {
          indexInto(esIndex / esType).doc(item)
        }.await
      })
  }
}
