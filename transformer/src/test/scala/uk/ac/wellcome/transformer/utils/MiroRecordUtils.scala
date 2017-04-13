package uk.ac.wellcome.transformer.utils

import com.amazonaws.services.dynamodbv2.model.{AttributeValue, Record, StreamRecord}

trait MiroRecordUtils extends RecordUtils {

  def createValidMiroRecord(MiroID: String,
                            MiroCollection: String,
                            data: String): Record = {
    val record = new Record()
    val streamRecord =
      createMiroStreamRecord(MiroID, MiroCollection, data)
    record.withDynamodb(streamRecord)
    record
  }

  private def createMiroStreamRecord(MiroID: String,
                                     MiroCollection: String,
                                     data: String) = {
    val streamRecord = new StreamRecord()
    streamRecord.addNewImageEntry("MiroID", new AttributeValue(MiroID))
    streamRecord.addNewImageEntry("MiroCollection", new AttributeValue(MiroCollection))
    streamRecord.addNewImageEntry("data", new AttributeValue(data))
    streamRecord
  }
}
