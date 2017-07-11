package uk.ac.wellcome.platform.idminter.steps

import com.twitter.inject.Logging
import uk.ac.wellcome.models.Work
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future

object WorkExtractor extends Logging {

  def toWork(message: SQSMessage): Future[Work] =
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

  private def tryExtractinWork(message: SQSMessage) = {
    info(s"Extracting Work from SQSMessage $message")
    JsonUtil.fromJson[Work](message.body)
  }
}
