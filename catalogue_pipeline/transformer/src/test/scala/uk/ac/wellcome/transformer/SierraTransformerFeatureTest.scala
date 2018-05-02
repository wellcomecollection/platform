package uk.ac.wellcome.transformer

import java.time.Instant

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
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

class SierraTransformerFeatureTest
    extends FunSpec
    with Matchers
    with SQS
    with SNS
    with S3
    with fixtures.Server
    with Eventually
    with ExtendedPatience
    with TransformableMessageUtils {

  it("transforms sierra records and publishes the result to the given topic") {
    val id = "1001001"
    val title = "A pot of possums"
    val lastModifiedDate = Instant.now()

    withLocalSnsTopic { topicArn =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { bucket =>
          val sierraHybridRecordMessage =
            hybridRecordSqsMessage(
              message = createValidSierraTransformableJson(
                id = id,
                title = title,
                lastModifiedDate = lastModifiedDate
              ),
              sourceName = "sierra",
              version = 1,
              s3Client = s3Client,
              bucket = bucket
            )

          sqsClient.sendMessage(
            queue.url,
            JsonUtil.toJson(sierraHybridRecordMessage).get
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
                identifierScheme = IdentifierSchemes.sierraSystemNumber,
                ontologyType = "Work",
                value = "b10010014"
              )

              val sierraIdentifier = SourceIdentifier(
                identifierScheme = IdentifierSchemes.sierraIdentifier,
                ontologyType = "Work",
                value = id
              )

              snsMessages.map { snsMessage =>
                val actualWork =
                  JsonUtil
                    .fromJson[UnidentifiedWork](snsMessage.message)
                    .get

                actualWork.sourceIdentifier shouldBe sourceIdentifier
                actualWork.title shouldBe Some(title)
                actualWork.identifiers shouldBe List(
                  sourceIdentifier,
                  sierraIdentifier)
              }
            }
          }
        }
      }
    }
  }
}
