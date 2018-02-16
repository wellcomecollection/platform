package uk.ac.wellcome.platform.reindex_worker.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.reindex_worker.models.{
  CompletedReindexJob,
  ReindexJob
}
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class ReindexerWorkerService @Inject()(
  targetService: ReindexService,
  reader: SQSReader,
  snsWriter: SNSWriter,
  system: ActorSystem,
  metrics: MetricsSender
) extends SQSWorker(reader, system, metrics) {

  override def processMessage(message: SQSMessage): Future[Unit] = Future {
    fromJson[ReindexJob](message.body) match {
      case Success(reindexJob) =>
        for {
          _ <- targetService.runReindex(reindexJob = reindexJob)
          message <- Future.fromTry(toJson(CompletedReindexJob(reindexJob)))
          result <- snsWriter.writeMessage(
            subject = s"source: ${this.getClass.getSimpleName}.processMessage",
            message = message
          )
        } yield result

      case Failure(err) => throw GracefulFailureException(err)
    }
  }
}
