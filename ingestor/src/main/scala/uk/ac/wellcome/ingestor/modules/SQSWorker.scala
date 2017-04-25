package uk.ac.wellcome.platform.ingestor.modules

import akka.actor.ActorSystem
import com.amazonaws.services.sqs.model.{Message => AwsSQSMessage}
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.ingestor.services.IdentifiedUnifiedItemIndexer
import uk.ac.wellcome.sqs.SQSReader
import uk.ac.wellcome.utils.{JsonUtil, TryBackoff}

import scala.util.{Failure, Success}

object SQSWorker extends TwitterModule with TryBackoff {
  private val esIndex = flag[String]("es.index", "records", "ES index name")
  private val esType = flag[String]("es.type", "item", "ES document type")

  override def singletonStartup(injector: Injector) {
    info("Starting SQS worker")

    val system = injector.instance[ActorSystem]

    val sqsReader = injector.instance[SQSReader]
    val indexer = injector.instance[IdentifiedUnifiedItemIndexer]

    run(() => processMessages(sqsReader, indexer), system)
  }

  private def processMessages(
    sqsReader: SQSReader,
    indexer: IdentifiedUnifiedItemIndexer): Unit = {
    sqsReader.retrieveAndDeleteMessages { message =>
      extractMessage(message).map { sqsMessage =>
        indexer.indexIdentifiedUnifiedItem(sqsMessage.body)
      }
    }
  }

  private def extractMessage(sqsMessage: AwsSQSMessage): Option[SQSMessage] =
    JsonUtil.fromJson[SQSMessage](sqsMessage.getBody) match {
      case Success(m) => Some(m)
      case Failure(e) => {
        error("Invalid message structure (not via SNS?)", e)
        None
      }
    }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    system.terminate()
  }
}
