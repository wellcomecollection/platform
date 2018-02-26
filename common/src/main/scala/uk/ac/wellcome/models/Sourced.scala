package uk.ac.wellcome.models

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.error.DynamoReadError

trait Sourced  extends Id {
  val sourceId: String
  val sourceName: String
  val id: String = Sourced.id(sourceName, sourceId)
}

object Sourced {
  implicit def toSourcedDynamoFormatWrapper[T <: Sourced](
    implicit dynamoFormat: DynamoFormat[T]): SourcedDynamoFormatWrapper[T] =
    new SourcedDynamoFormatWrapper[T](dynamoFormat)

  def id(sourceName: String, sourceId: String) = s"$sourceName/$sourceId"
}

class SourcedDynamoFormatWrapper[T <: Sourced](dynamoFormat: DynamoFormat[T]) {
  val enrichedDynamoFormat = new DynamoFormat[T] {
    override def read(av: AttributeValue): Either[DynamoReadError, T] =
      dynamoFormat.read(av)

    override def write(t: T): AttributeValue =
      dynamoFormat.write(t).addMEntry("id", new AttributeValue(t.id))
  }
}
