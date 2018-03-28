package uk.ac.wellcome.transformer

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.CalmTransformable
import uk.ac.wellcome.models.{
  IdentifierSchemes,
  SourceIdentifier,
  UnidentifiedWork
}
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
      withLocalSqsQueue { queueUrl =>
        withLocalS3Bucket { bucketName =>
          val calmHybridRecordMessage = hybridRecordSqsMessage(
            message = JsonUtil.toJson(calmTransformable).get,
            sourceName = "calm",
            version = 1,
            s3Client = s3Client,
            bucketName = bucketName
          )

          val flags: Map[String, String] = Map(
            "aws.metrics.namespace" -> "sierra-transformer"
          ) ++ s3LocalFlags(bucketName) ++ snsLocalFlags(topicArn) ++ sqsLocalFlags(
            queueUrl)

          withServer(flags) { _ =>
            sqsClient.sendMessage(
              queueUrl,
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
