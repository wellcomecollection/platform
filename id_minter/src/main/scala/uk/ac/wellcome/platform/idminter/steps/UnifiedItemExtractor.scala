package uk.ac.wellcome.platform.idminter.steps

import com.amazonaws.services.sqs.model.Message
import com.twitter.inject.Logging
import uk.ac.wellcome.models.UnifiedItem
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object UnifiedItemExtractor extends Logging {

  def toUnifiedItem(message: Message): Future[UnifiedItem] = Future{
    tryExtractinUnifiedItem(message)
  }.map {
    case Success(unifiedItem) =>
      info(s"Successfully extracted unified item $unifiedItem")
      unifiedItem
    case Failure(e) =>
      error("Failed extracting Unified Item from AWS message", e)
      throw e
  }

  private def tryExtractinUnifiedItem(message: Message) = {
    info(s"Parsing SQSMessage ${message.getBody}")
    JsonUtil.fromJson[SQSMessage](message.getBody).flatMap { sqsMessage =>
      info(s"Extracting UnifiedItem from SQSMessage $sqsMessage")
      JsonUtil.fromJson[UnifiedItem](sqsMessage.body)
    }
  }
}
