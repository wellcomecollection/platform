package uk.ac.wellcome.platform.idminter.steps

import com.amazonaws.services.sqs.model.Message
import com.twitter.inject.Logging
import uk.ac.wellcome.models.UnifiedItem
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future

object UnifiedItemExtractor extends Logging {

  def toUnifiedItem(message: Message): Future[UnifiedItem] =
    Future
      .fromTry {
        tryExtractinUnifiedItem(message)
      }
      .map { unifiedItem =>
        info(s"Successfully extracted unified item $unifiedItem")
        unifiedItem
      } recover {
      case e: Throwable =>
        error("Failed extracting unified item from AWS message", e)
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
