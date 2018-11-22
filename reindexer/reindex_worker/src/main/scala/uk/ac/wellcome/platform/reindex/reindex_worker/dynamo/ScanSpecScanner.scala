package uk.ac.wellcome.platform.reindex.reindex_worker.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.document._
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

/** Implements a wrapper for DynamoDB Scan operations using a ScanSpec.
  *
  * This wrapper provides a list of JSON strings, which can be sent directly
  * to a downstream application.
  *
  * For the options allowed by ScanSpec, see:
  * https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/document/spec/ScanSpec.html
  */
class ScanSpecScanner(dynamoDBClient: AmazonDynamoDB)(
  implicit ec: ExecutionContext) {

  val dynamoDB = new DynamoDB(dynamoDBClient)

  /** Run a Scan specified by a ScanSpec.
    *
    * Note that this returns a Future[List], so results will be cached in-memory.
    * Design your spec accordingly.
    */
  def scan(scanSpec: ScanSpec, tableName: String): Future[List[String]] = {
    for {
      table <- Future.successful { dynamoDB.getTable(tableName) }
      scanResult: ItemCollection[ScanOutcome] <- Future { table.scan(scanSpec) }
      items: List[Item] = scanResult.asScala.toList
      jsonStrings = items.map { _.toJSON }
    } yield jsonStrings
  }
}
