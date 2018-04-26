package uk.ac.wellcome.transformer

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.CalmTransformable
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.transformer.utils.TransformableMessageUtils
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.work_model.{IdentifierSchemes, SourceIdentifier, UnidentifiedWork}

class CalmTransformerFeatureTest
    extends FunSpec
    with Matchers
    with SQS
    with SNS
    with S3
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

          val flags: Map[String, String] = Map(
            "aws.metrics.namespace" -> "sierra-transformer"
          ) ++ s3LocalFlags(bucket) ++ snsLocalFlags(topicArn) ++ sqsLocalFlags(
            queue)

          withServer(flags) { _ =>
            sqsClient.sendMessage(
              queue.url,
              JsonUtil.toJson(calmHybridRecordMessage).get
            )

            eventually {
              val snsMessages = listMessagesReceivedFromSNS(topicArn)
              snsMessages should have size 1
              assertSNSMessageContainsCalmDataWith(
                snsMessage = snsMessages.head,
                AccessStatus = Some("public")
              )
            }
          }
        }
      }
    }
  }

  private def assertSNSMessageContainsCalmDataWith(
    snsMessage: MessageInfo,
    AccessStatus: Option[String]): Any = {

    val sourceIdentifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.calmPlaceholder,
      ontologyType = "Work",
      value = "value"
    )

    //currently for calm data we only output hardcoded sample values
    snsMessage.message shouldBe JsonUtil
      .toJson(
        UnidentifiedWork(
          title = Some("placeholder title"),
          sourceIdentifier = sourceIdentifier,
          version = 1,
          identifiers = List(sourceIdentifier)))
      .get
  }
}
