package uk.ac.wellcome.utils

import scala.collection.JavaConversions._

import com.amazonaws.services.dynamodbv2._
import com.amazonaws.services.dynamodbv2.document._
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput


trait DynamoUpdateWriteCapacityCapable {
  val client: AmazonDynamoDB

  def updateWriteCapacity(tableName: String, writeCapacity: Long) = {
    val db = new DynamoDB(client)
    val table = db.getTable(tableName)
    table.describe()

    val readCapacity = table
      .getDescription()
      .getProvisionedThroughput()
      .getReadCapacityUnits()

    val newThroughput = new ProvisionedThroughput(readCapacity, writeCapacity)

    table
      .getDescription()
      .getGlobalSecondaryIndexes()
      .map { index => table.getIndex(index.getIndexName()) }
      .map { index => index.updateGSI(newThroughput) }

    table.updateTable(newThroughput)
  }
}
