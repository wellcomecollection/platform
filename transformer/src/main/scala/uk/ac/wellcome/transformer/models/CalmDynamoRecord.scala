package uk.ac.wellcome.platform.transformer.models

trait Transformable {
  def tranform[T]: CleanedRecord
}


case class CalmDynamoRecord(
  RecordID: String,
  RecordType: String,
  AltRefNo: Option[String],
  RefNo: Option[String],
  data: Option[String]
) extends Transformable {
  def transform[ExampleRecord](): CleanedRecord {
    // abracadabra goes here
    CleanedRecord(identifier)
  }
}

