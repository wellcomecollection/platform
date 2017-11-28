package uk.ac.wellcome.platform.sierra_bib_merger.services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import com.amazonaws.services.dynamodbv2.model.PutItemResult
import com.google.inject.Inject
import com.gu.scanamo.ScanamoAsync
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.platform.sierra_bib_merger.models.MergedSierraObject

import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class SierraBibMergerUpdaterService @Inject()(
   dynamoDBClient: AmazonDynamoDBAsync,
   metrics: MetricsSender,
   @Flag("bibMerger.dynamo.tableName") tableName: String){

  def update(mergedSierraObject: MergedSierraObject): Future[PutItemResult] =
    ScanamoAsync.put[MergedSierraObject](dynamoDBClient)(tableName)(mergedSierraObject)

}
