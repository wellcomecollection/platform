package uk.ac.wellcome.transformer.utils

import com.amazonaws.services.dynamodbv2.model.{
  AttributeValue,
  Record,
  StreamRecord
}

trait RecordUtils {

  def createInvalidRecord: Record = {
    val record = new Record()
    val streamRecord = new StreamRecord()
    streamRecord.addNewImageEntry("something",
                                  new AttributeValue("something-else"))
    record.withDynamodb(streamRecord)
    record
  }
}
