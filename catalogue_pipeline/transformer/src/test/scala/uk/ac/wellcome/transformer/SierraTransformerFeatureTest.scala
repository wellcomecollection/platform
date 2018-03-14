package uk.ac.wellcome.transformer

import java.time.Instant

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.models.{
  IdentifierSchemes,
  SourceIdentifier,
  UnidentifiedWork
}
import uk.ac.wellcome.test.fixtures.{S3, SnsFixtures, SqsFixtures}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.transformer.utils.TransformableMessageUtils
import uk.ac.wellcome.utils.JsonUtil

class SierraTransformerFeatureTest
    extends FunSpec
    with Matchers
    with SqsFixtures
    with SnsFixtures
    with S3
    with fixtures.Server
    with Eventually
    with ExtendedPatience
    with TransformableMessageUtils {

  it("transforms sierra records and publishes the result to the given topic") {
    val id = "b001"
    val title = "A pot of possums"
    val lastModifiedDate = Instant.now()

    withLocalSnsTopic { topicArn =>
      withLocalSqsQueue { queueUrl =>
        withLocalS3Bucket { bucketName =>

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
              bucketName = bucketName
            )

          sqsClient.sendMessage(
            queueUrl,
            JsonUtil.toJson(sierraHybridRecordMessage).get
          )

          val flags: Map[String, String] = Map(
            "aws.sqs.queue.url" -> queueUrl,
            "aws.sns.topic.arn" -> topicArn,
            "aws.s3.bucketName" -> bucketName,
            "aws.sqs.waitTime" -> "1",
            "aws.metrics.namespace" -> "sierra-transformer"
          ) ++ s3LocalFlags ++ snsLocalFlags ++ sqsLocalFlags

          withServer(flags) { _ => eventually {
              val snsMessages = listMessagesReceivedFromSNS(topicArn)
              snsMessages should have size 1

              val sourceIdentifier = SourceIdentifier(
                IdentifierSchemes.sierraSystemNumber,
                id
              )

              val actualWork =
                JsonUtil
                  .fromJson[UnidentifiedWork](snsMessages.head.message)
                  .get

              actualWork.sourceIdentifier shouldBe sourceIdentifier
              actualWork.title shouldBe Some(title)
              actualWork.identifiers shouldBe List(sourceIdentifier)
            }
          }
        }
      }
    }
  }
}
