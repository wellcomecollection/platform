package uk.ac.wellcome.platform.idminter.steps

import com.amazonaws.services.sqs.model.Message
import com.twitter.inject.Logging
import uk.ac.wellcome.models.Work
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future

object WorkExtractor extends Logging {

  def toWork(message: Message): Future[Work] =
    Future
      .fromTry {
        tryExtractinWork(message)
      }
      .map { work =>
        info(s"Successfully extracted unified item $work")
        work
      } recover {
      case e: Throwable =>
        error("Failed extracting unified item from AWS message", e)
        throw e
    }

  private def tryExtractinWork(message: Message) = {
    info(s"Parsing SQSMessage ${message.getBody}")
    JsonUtil.fromJson[SQSMessage](message.getBody).flatMap { sqsMessage =>
      info(s"Extracting Work from SQSMessage $sqsMessage")
      JsonUtil.fromJson[Work](sqsMessage.body)
    }
  }
}
