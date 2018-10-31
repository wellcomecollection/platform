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

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

/** Implements a wrapper for DynamoDB Scan operations using a ScanSpec.
  *
  * A ScanSpec is a way to fully specify the parameters of a Scan operation, which
  * is very flexible -- but then we lose the serialisation of Scanamo case classes.
  * This class combines the best of both worlds: it allows the flexibility of using
  * ScanSpec, but also handles case class serialisation.
  *
  * For the options allowed by ScanSpec, see:
  * https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/document/spec/ScanSpec.html
  */
class ScanSpecScanner @Inject()(dynamoDBClient: AmazonDynamoDB)(
  implicit ec: ExecutionContext) {

  val dynamoDB = new DynamoDB(dynamoDBClient)

  /** Run a Scan specified by a ScanSpec.
    *
    * Note that this returns a Future[List], so results will be cached in-memory.
    * Design your spec accordingly.
    */
  def scan[T](scanSpec: ScanSpec, tableName: String)(
    implicit dynamoFormat: DynamoFormat[T])
    : Future[List[Either[DynamoReadError, T]]] = {
    val table = dynamoDB.getTable(tableName)

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
