package uk.ac.wellcome.platform.reindexer.services

import javax.inject.Inject

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.{Scanamo, Table}
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.models.MiroTransformable

class MiroReindexTargetService @Inject()(
  dynamoDBClient: AmazonDynamoDB,
  @Flag("reindex.target.tableName") reindexTargetTableName: String)
    extends ReindexTargetService[MiroTransformable](dynamoDBClient) {

  override val transformableTable: Table[MiroTransformable] =
    Table[MiroTransformable](reindexTargetTableName)

  override val scanamoQuery: ScanamoQuery =
    Scanamo.queryIndex[MiroTransformable](dynamoDBClient) _
}
