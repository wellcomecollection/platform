package ac.uk.wellcome.vhs_to_sns

import com.amazonaws.services.dynamodbv2.model.StreamRecord
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord
import com.gu.scanamo.DynamoFormat
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import io.circe.parser._
import io.circe.generic.auto._

import scala.collection.JavaConversions._

class MainTest extends FunSpec with Messaging {
  it("receives a dynamodb event and sends it to sns") {
    withLocalSnsTopic { topic =>
      sys.props.put("endpoint", localSNSEndpointUrl)
      sys.props.put("accessKey", accessKey)
      sys.props.put("secretKey", secretKey)

      sys.props.put("TOPIC_ARN", topic.arn)
      val bucketName = "bucket"
      sys.props.put("BUCKET_NAME", bucketName)
      val s3Key = "key.json"
      val thing = HybridRecord(id = "id", version = 1, s3key = s3Key)

      val dynamoFormat = implicitly[DynamoFormat[HybridRecord]]
      val streamRecord =
        new StreamRecord().withNewImage(dynamoFormat.write(thing).getM)
      val record = new DynamodbStreamRecord()
      record.setDynamodb(streamRecord)
      val event = new DynamodbEvent()
      event.setRecords(List(record))

      val main = new Main()
      main.toMessagePointer(event)

      eventually {
        val messages = listMessagesReceivedFromSNS(topic)

        messages.map(message =>
          decode[MessagePointer](message.message).right.get) should contain only (MessagePointer(
          S3ObjectLocation(bucketName, s3Key)))
      }
    }
  }
}
