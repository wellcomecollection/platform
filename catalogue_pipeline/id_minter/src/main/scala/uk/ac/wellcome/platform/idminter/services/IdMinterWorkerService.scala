package uk.ac.wellcome.platform.idminter.services

import akka.Done
import io.circe.Json
import uk.ac.wellcome.messaging.message.{MessageStream, MessageWriter}
import uk.ac.wellcome.platform.idminter.config.models.{
  IdentifiersTableConfig,
  RDSClientConfig
}
import uk.ac.wellcome.platform.idminter.database.TableProvisioner
import uk.ac.wellcome.platform.idminter.steps.IdEmbedder
import uk.ac.wellcome.typesafe.Runnable

import scala.concurrent.{ExecutionContext, Future}

class IdMinterWorkerService(
  idEmbedder: IdEmbedder,
  writer: MessageWriter[Json],
  messageStream: MessageStream[Json],
  rdsClientConfig: RDSClientConfig,
  identifiersTableConfig: IdentifiersTableConfig
)(implicit ec: ExecutionContext)
    extends Runnable {

  def run(): Future[Done] = {
    val tableProvisioner = new TableProvisioner(
      rdsClientConfig = rdsClientConfig
    )

    tableProvisioner.provision(
      database = identifiersTableConfig.database,
      tableName = identifiersTableConfig.tableName
    )

    messageStream.foreach(this.getClass.getSimpleName, processMessage)
  }

  def processMessage(json: Json): Future[Unit] =
    for {
      identifiedJson <- idEmbedder.embedId(json)
      _ <- writer.write(
        message = identifiedJson,
        subject = s"source: ${this.getClass.getSimpleName}.processMessage"
      )
    } yield ()
}
