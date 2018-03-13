package uk.ac.wellcome.transformer

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.CalmTransformable
import uk.ac.wellcome.models.{IdentifierSchemes, SourceIdentifier, UnidentifiedWork}
import uk.ac.wellcome.test.fixtures.{MessageInfo, S3, SnsFixtures, SqsFixtures}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.transformer.utils.TransformableMessageUtils
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

class CalmTransformerFeatureTest
  extends FunSpec
    with Matchers
    with SqsFixtures
    with SnsFixtures
    with S3
    with fixtures.Server
    with Eventually
    with ExtendedPatience
    with TransformableMessageUtils {

  it("transforms miro records and publishes the result to the given topic") {
    withLocalSnsTopic { topicArn =>
      withLocalSqsQueue { queueUrl =>
        withLocalS3Bucket { bucketName =>

          val flags: Map[String, String] = Map(
            "aws.sqs.queue.url" -> queueUrl,
            "aws.sns.topic.arn" -> topicArn,
            "aws.s3.bucketName" -> bucketName,
            "aws.sqs.waitTime" -> "1",
            "aws.metrics.namespace" -> "sierra-transformer"

          ) ++ s3LocalFlags ++ snsLocalFlags ++ sqsLocalFlags

          withServer(flags) { _ =>

            val calmTransformable =
              CalmTransformable(
                sourceId = "RecordID1",
                RecordType = "Collection",
                AltRefNo = "AltRefNo1",
                RefNo = "RefNo1",
                data = """{"AccessStatus": ["public"]}""")

            val calmHybridRecordMessage = hybridRecordSqsMessage(
              message = JsonUtil.toJson(calmTransformable).get,
              sourceName = "calm",
              version = 1,
              s3Client = s3Client,
              bucketName = bucketName
            )

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
      IdentifierSchemes.calmPlaceholder,
      "value"
    )

    //currently for calm data we only output hardcoded sample values
    snsMessage.message shouldBe JsonUtil
      .toJson(
        UnidentifiedWork(
          title = Some("placeholder title for a Calm record"),
          sourceIdentifier = sourceIdentifier,
          version = 1,
          identifiers = List(sourceIdentifier)))
      .get
  }
}
