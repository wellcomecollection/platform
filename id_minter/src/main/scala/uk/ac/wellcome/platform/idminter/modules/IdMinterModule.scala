package uk.ac.wellcome.platform.idminter.modules

import com.twitter.inject.Injector
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{IdentifiedWork, Work}
import uk.ac.wellcome.platform.idminter.steps.{IdentifierGenerator, WorkExtractor}
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.sqs.SQSWorker
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

object IdMinterModule extends SQSWorker {
  val snsSubject = "identified-item"
  val database = flag[String]("aws.rds.identifiers.database",
    "",
    "Name of the identifiers database")
  val tableName = flag[String]("aws.rds.identifiers.table",
    "",
    "Name of the identifiers table")

  private def toIdentifiedWorkJson(work: Work, canonicalId: String) = {
    JsonUtil.toJson(IdentifiedWork(canonicalId, work)).get
  }

  override def processMessage(message: SQSMessage,
                              injector: Injector): Future[Unit] = {
    val idGenerator = injector.instance[IdentifierGenerator]
    val snsWriter = injector.instance[SNSWriter]

    for {
      work <- WorkExtractor.toWork(message)
      canonicalId <- idGenerator.generateId(work)
      _ <- snsWriter.writeMessage(toIdentifiedWorkJson(work, canonicalId),
        Some(snsSubject))
    } yield ()
  }
}