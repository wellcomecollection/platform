package uk.ac.wellcome.platform.reindex.reindex_worker.dynamo

import java.util

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.document.internal.InternalUtils
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec
import com.amazonaws.services.dynamodbv2.document.{DynamoDB, Item}
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.google.inject.Inject
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.{DynamoFormat, ScanamoFree}
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.dynamo.DynamoConfig

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

/** Implements a wrapper for Parallel Scans of a DynamoDB table.
  * In a nutshell, this operation lets you have multiple parallel workers
  * that Scan the rows of a DynamoDB table, and DynamoDB handles the
  * problem of dividing up rows between the different workers.
  *
  * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Scan.html#Scan.ParallelScan
  */
class ParallelScanner @Inject()(
  dynamoDBClient: AmazonDynamoDB,
  dynamoConfig: DynamoConfig
)(implicit ec: ExecutionContext)
    extends Logging {

  val dynamoDB = new DynamoDB(dynamoDBClient)

  /** Run a Parallel Scan for a single worker.
    *
    * To perform a parallel scan, each worker should call this method with
    * two parameters:
    *
    * @param segment       Which segment this work is scanning.  Each worker should
    *                      choose a different segment.  This parameter is 0-indexed.
    * @param totalSegments How many segments there are in total.  Each worker
    *                      should use the same value.
    *
    * Note that this returns a Future[List], so results will be cached in-memory.
    * Choose segment count accordingly.
    */
  def scan[T](segment: Int, totalSegments: Int)(
    implicit dynamoFormat: DynamoFormat[T])
    : Future[List[Either[DynamoReadError, T]]] = {

    // Create the ScanSpec configuration and the DynamoDB table.  This is
    // based on the Java example of a Parallel Scan from the AWS docs:
    // https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ScanJavaDocumentAPI.html
    //
    val scanSpec = new ScanSpec()
      .withTotalSegments(totalSegments)
      .withSegment(segment)

    val table = dynamoDB.getTable(dynamoConfig.table)

    Future {

      // Actually run the Scan operation.  Again, this is based on the
      // Java example from the AWS docs.
      val itemCollection: List[Item] = table
        .scan(scanSpec)
        .asScala
        .toList

      itemCollection.map { item: Item =>
        // Convert the "Item" (a collection of attributes) into a map of
        // strings to AttributeValues, which we need for Scanamo.  Here we're
        // using the internal utilities from the AWS DynamoDB SDK.
        //
        // I got this from SO, although it's an incidental part of the answer:
        // https://stackoverflow.com/a/27538483/1558022
        //
        val attributeValues: util.Map[String, AttributeValue] =
          InternalUtils.toAttributeValues(item)

        // Take the Map[String, AttributeValue], and convert it into an
        // instance of the case class `T`.  This is using a Scanamo helper --
        // I worked this out by looking at [[ScanamoFree.get]].
        //
        // https://github.com/scanamo/scanamo/blob/12554b8e24ef8839d5e9dd9a4f42ae130e29b42b/scanamo/src/main/scala/com/gu/scanamo/ScanamoFree.scala#L62
        //
        ScanamoFree.read[T](attributeValues)
      }
    }
  }
}
