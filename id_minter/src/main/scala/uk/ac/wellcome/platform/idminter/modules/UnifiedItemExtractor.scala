package uk.ac.wellcome.platform.idminter.modules

import com.amazonaws.services.sqs.model.Message
import com.twitter.inject.Logging
import uk.ac.wellcome.models.UnifiedItem
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global


class UnifiedItemExtractor extends Logging {

  def toUnifiedItem(message: Message): Future[UnifiedItem] = Future{
    info(s"Parsing SQSMessage ${message.getBody}")
    JsonUtil.fromJson[SQSMessage](message.getBody).flatMap {sqsMessage =>
      info(s"Extracting UnifiedItem from SQSMessage $sqsMessage")
      JsonUtil.fromJson[UnifiedItem](sqsMessage.body)}
  }.map {
    case Success(unifiedItem) =>
      info("Successfully extractedUnified item")
      unifiedItem
    case Failure(e) =>
      error("Failed extracting Unified Item from Aws message", e)
      throw e
  }

}
