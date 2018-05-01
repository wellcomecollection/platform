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
import uk.ac.wellcome.models.work.internal.{IdentifierSchemes, SourceIdentifier, UnidentifiedWork}
import uk.ac.wellcome.models.work.internal.{
  IdentifierSchemes,
  SourceIdentifier,
  UnidentifiedWork
}
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.test.fixtures._
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

    withLocalSnsTopic { topicArn =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { bucket =>
          val calmHybridRecordMessage = hybridRecordSqsMessage(
            message = JsonUtil.toJson(calmTransformable).get,
            sourceName = "calm",
            version = 1,
            s3Client = s3Client,
            bucket = bucket
          )

          sqsClient.sendMessage(
            queue.url,
            JsonUtil.toJson(calmHybridRecordMessage).get
          )

          val flags: Map[String, String] = Map(
            "aws.metrics.namespace" -> "sierra-transformer"
          ) ++ s3LocalFlags(bucket) ++ snsLocalFlags(topicArn) ++ sqsLocalFlags(
            queue)

          withServer(flags) { _ =>
            eventually {
              val snsMessages = listMessagesReceivedFromSNS(topicArn)
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
