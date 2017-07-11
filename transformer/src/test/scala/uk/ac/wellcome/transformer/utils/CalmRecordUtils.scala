package uk.ac.wellcome.transformer.utils

import com.amazonaws.services.dynamodbv2.model.{
  AttributeValue,
  Record,
  StreamRecord
}

trait CalmRecordUtils extends RecordUtils {

  def createValidCalmRecord(RecordID: String,
                            RecordType: String,
                            AltRefNo: String,
                            RefNo: String,
                            data: String): Record = {
    val record = new Record()
    val streamRecord =
      createCalmStreamRecord(RecordID, RecordType, RefNo, AltRefNo, data)
    record.withDynamodb(streamRecord)
    record
  }

  private def createCalmStreamRecord(RecordID: String,
                                     RecordType: String,
                                     RefNo: String,
                                     AltRefNo: String,
                                     data: String) = {
    val streamRecord = new StreamRecord()
    streamRecord.addNewImageEntry("RecordID", new AttributeValue(RecordID))
    streamRecord.addNewImageEntry("RecordType", new AttributeValue(RecordType))
    streamRecord.addNewImageEntry("RefNo", new AttributeValue(RefNo))
    streamRecord.addNewImageEntry("AltRefNo", new AttributeValue(AltRefNo))
    streamRecord.addNewImageEntry("data", new AttributeValue(data))
    streamRecord.addNewImageEntry("ReindexShard",
                                  new AttributeValue("default"))
    streamRecord.addNewImageEntry("ReindexVersion",
                                  new AttributeValue().withN("0"))
    streamRecord
  }
}
