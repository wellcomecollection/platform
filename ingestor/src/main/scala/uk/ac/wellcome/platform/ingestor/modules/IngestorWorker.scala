package uk.ac.wellcome.platform.ingestor.modules

import com.twitter.inject.Injector
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.ingestor.services.IdentifiedWorkIndexer
import uk.ac.wellcome.sqs.SQSWorker
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

object IngestorWorker extends SQSWorker {
  private val esIndex = flag[String]("es.index", "records", "ES index name")
  private val esType = flag[String]("es.type", "item", "ES document type")

  override def processMessage(message: SQSMessage, injector: Injector): Future[Unit] = {
    val indexer = injector.instance[IdentifiedWorkIndexer]

    indexer.indexIdentifiedWork(message.body).map(_=>())
  }
}
