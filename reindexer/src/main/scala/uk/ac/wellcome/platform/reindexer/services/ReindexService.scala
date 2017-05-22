package uk.ac.wellcome.platform.reindexer.services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.Scanamo
import uk.ac.wellcome.models.Reindex
import uk.ac.wellcome.models.aws.DynamoConfig

class ReindexService @Inject()(dynamoDBClient: AmazonDynamoDB,
                               dynamoConfig: DynamoConfig) {

  def getIndicesForReindex = getIndices.filter {
    case Reindex(_, requested, current) if requested > current => true
    case _ => false
  }

  def getIndices: List[Reindex] = {
    Scanamo.scan[Reindex](dynamoDBClient)(dynamoConfig.table).map {
      case Right(reindexes) => reindexes
      case _ => throw new RuntimeException("nope")
    }
  }
}