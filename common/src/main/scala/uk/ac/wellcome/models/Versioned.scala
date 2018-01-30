package uk.ac.wellcome.models

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.error.DynamoReadError

trait Versioned {
  val version: Int
  val sourceId: String
  val sourceName: String

  val id: String = s"$sourceName/$sourceId"
}

object Versioned{
  implicit def toVersionedDynamoFormat[T <: Versioned](implicit dynamoFormat: DynamoFormat[T]): VersionedDynamoFormat[T] = new VersionedDynamoFormat[T](dynamoFormat)
}

class VersionedDynamoFormat[T <: Versioned](dynamoFormat: DynamoFormat[T]){
  val enrichedDynamoFormat = new DynamoFormat[T] {
    override def read(av: AttributeValue): Either[DynamoReadError, T] = dynamoFormat.read(av)

    override def write(t: T): AttributeValue = dynamoFormat.write(t).addMEntry("id", new AttributeValue(t.id))
  }
}

trait VersionUpdater[T <: Versioned] {
  def updateVersion(versioned: T, newVersion: Int): T
}
