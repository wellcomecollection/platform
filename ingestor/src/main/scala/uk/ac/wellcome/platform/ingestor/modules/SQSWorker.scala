package uk.ac.wellcome.platform.ingestor.modules

import akka.actor.ActorSystem
import com.amazonaws.services.sqs.model.{Message => AwsSQSMessage}
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.ingestor.services.IdentifiedWorkIndexer
import uk.ac.wellcome.sqs.SQSReader
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.{JsonUtil, TryBackoff}

import scala.concurrent.Future
import scala.util.Try

object SQSWorker extends TwitterModule with TryBackoff {
  private val esIndex = flag[String]("es.index", "records", "ES index name")
  private val esType = flag[String]("es.type", "item", "ES document type")

  override def singletonStartup(injector: Injector) {
    info("Starting SQS worker")

    val system = injector.instance[ActorSystem]
    val sqsReader = injector.instance[SQSReader]
    val indexer = injector.instance[IdentifiedWorkIndexer]

    run(() => processMessages(sqsReader, indexer), system)
  }

  private def processMessages(
    sqsReader: SQSReader,
    indexer: IdentifiedWorkIndexer): Unit = {
    sqsReader.retrieveAndDeleteMessages { message =>
      Future.fromTry(extractMessage(message)).flatMap { sqsMessage =>
        indexer.indexIdentifiedWork(sqsMessage.body).map(_=>())
      }
    }
  }

  private def extractMessage(sqsMessage: AwsSQSMessage): Try[SQSMessage] =
    JsonUtil.fromJson[SQSMessage](sqsMessage.getBody).recover{
      case e: Throwable =>
        error("Invalid message structure (not via SNS?)", e)
        throw e
    }


  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")
    cancelRun()
    val system = injector.instance[ActorSystem]
    system.terminate()
  }
}
