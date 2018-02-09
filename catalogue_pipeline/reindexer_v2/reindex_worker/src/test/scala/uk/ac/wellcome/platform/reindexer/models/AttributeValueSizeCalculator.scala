package uk.ac.wellcome.platform.reindexer.models

object AttributeValueSizeCalculator {
  // Copied from https://github.com/awslabs/dynamodb-janusgraph-storage-backend/blob/0.5.4/src/main/java/com/amazon/titan/diskstorage/dynamodb/DynamoDBDelegate.java#L1065
  // and automagically converted to scala
  import java.nio.charset.Charset

  private val UTF8 = Charset.forName("UTF8")
  import com.amazonaws.services.dynamodbv2.model.AttributeValue
  import java.util

  private val BASE_LOGICAL_SIZE_OF_NESTED_TYPES = 1
  private var LOGICAL_SIZE_OF_EMPTY_DOCUMENT = 3
  private val MAX_NUMBER_OF_BYTES_FOR_NUMBER = 21

  /** Calculate attribute value size */
  def calculateAttributeSizeInBytes(value: AttributeValue): Int = {
    var attrValSize = 0
    if (value == null) return attrValSize
    if (value.getB != null) {
      val b = value.getB
      attrValSize += b.remaining
    }
    else if (value.getS != null) {
      val s = value.getS
      attrValSize += s.getBytes(UTF8).length
    }
    else if (value.getN != null) attrValSize += MAX_NUMBER_OF_BYTES_FOR_NUMBER
    else if (value.getBS != null) {
      val bs = value.getBS
      import scala.collection.JavaConversions._
      for (b <- bs) {
        if (b != null) attrValSize += b.remaining
      }
    }
    else if (value.getSS != null) {
      val ss = value.getSS
      import scala.collection.JavaConversions._
      for (s <- ss) {
        if (s != null) attrValSize += s.getBytes(UTF8).length
      }
    }
    else if (value.getNS != null) {
      val ns = value.getNS
      import scala.collection.JavaConversions._
      for (n <- ns) {
        if (n != null) attrValSize += MAX_NUMBER_OF_BYTES_FOR_NUMBER
      }
    }
    else if (value.getBOOL != null) attrValSize += 1
    else if (value.getNULL != null) attrValSize += 1
    else if (value.getM != null) {
      import scala.collection.JavaConversions._
      for (entry <- value.getM.entrySet) {
        attrValSize += entry.getKey.getBytes(UTF8).length
        attrValSize += calculateAttributeSizeInBytes(entry.getValue)
        attrValSize += BASE_LOGICAL_SIZE_OF_NESTED_TYPES
      }
      attrValSize += LOGICAL_SIZE_OF_EMPTY_DOCUMENT
    }
    else if (value.getL != null) {
      val list = value.getL
      var i = 0
      while ( {
        i < list.size
      }) {
        attrValSize += calculateAttributeSizeInBytes(list.get(i))
        attrValSize += BASE_LOGICAL_SIZE_OF_NESTED_TYPES

        {
          i += 1; i - 1
        }
      }
      attrValSize += LOGICAL_SIZE_OF_EMPTY_DOCUMENT
    }
    attrValSize
  }
}
