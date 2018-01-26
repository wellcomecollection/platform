package uk.ac.wellcome.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.models.aws.{DynamoConfig, StoredRow}

abstract class DynamoS3StoreDao @Inject()(
  dynamoDbClient: AmazonDynamoDB,
  dynamoConfigs: Map[String, DynamoConfig]
) extends Logging {

  val tableConfigId: String

  private def dynamoConfig = dynamoConfigs.getOrElse(
    tableConfigId,
    throw new RuntimeException(
      s"DynamoS3StoreDao ($tableConfigId) DynamoDB config not available!"
    )
  )

  private def table = Table[StoredRow](dynamoConfig.table)

  private def scanamoExec[T](op: ScanamoOps[T]) =
    Scanamo.exec(dynamoDbClient)(op)

  private def putRecord(record: StoredRow) = ...

  def updateRecord(id: String, body: String): Future[Unit] = ...

  def getRecord(id: String): Future[Option[String]] = ...
}
