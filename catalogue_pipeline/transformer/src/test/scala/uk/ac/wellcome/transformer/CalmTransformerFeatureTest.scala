package uk.ac.wellcome.transformer

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.models.transformable.CalmTransformable
import uk.ac.wellcome.models.work.internal.{
  IdentifierSchemes,
  SourceIdentifier,
  UnidentifiedWork
}
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.transformer.utils.TransformableMessageUtils
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

class CalmTransformerFeatureTest
    extends FunSpec
    with Matchers
    with SQS
    with SNS
    with S3
    with Messaging
    with fixtures.Server
    with Eventually
    with ExtendedPatience
    with TransformableMessageUtils {

  it("transforms miro records and publishes the result to the given topic") {
    val calmTransformable =
      CalmTransformable(
        sourceId = "RecordID1",
        RecordType = "Collection",
        AltRefNo = "AltRefNo1",
        RefNo = "RefNo1",
        data = """{"AccessStatus": ["public"]}""")

    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withLocalS3Bucket { messageBucket =>
            val calmHybridRecordMessage = hybridRecordSqsMessage(
              message = JsonUtil.toJson(calmTransformable).get,
              sourceName = "calm",
              version = 1,
              s3Client = s3Client,
              bucket = storageBucket
            )

            sqsClient.sendMessage(
              queue.url,
              JsonUtil.toJson(calmHybridRecordMessage).get
            )

            val flags: Map[String, String] = s3LocalFlags(storageBucket) ++
              sqsLocalFlags(queue) ++ messageWriterLocalFlags(
              messageBucket,
              topic)

            withServer(flags) { _ =>
              eventually {
                val snsMessages = listMessagesReceivedFromSNS(topic)
                snsMessages.size should be >= 1

                val sourceIdentifier = SourceIdentifier(
                  identifierScheme = IdentifierSchemes.calmPlaceholder,
                  ontologyType = "Work",
                  value = "value"
                )

                snsMessages.map { snsMessage =>
                  val actualWork = get[UnidentifiedWork](snsMessage)

                  actualWork.sourceIdentifier shouldBe sourceIdentifier
                  actualWork.title shouldBe Some("placeholder title")
                  actualWork.identifiers shouldBe List(sourceIdentifier)
                }
              }
            }
          }
        }
      }
    }
  }
}
