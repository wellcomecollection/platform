package uk.ac.wellcome.transformer

import java.time.Instant

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.models.{IdentifierSchemes, SourceIdentifier, UnidentifiedWork, Work}
import uk.ac.wellcome.transformer.utils.{TransformableSQSMessageUtils, TransformerFeatureTest}
import uk.ac.wellcome.utils.JsonUtil

class SierraTransformerFeatureTest
    extends FunSpec
    with Matchers
    with TransformerFeatureTest
    with TransformableSQSMessageUtils {

  override lazy val bucketName: String =
    "test-sierra-transformer-feature-test-bucket"
  val queueUrl: String = createQueueAndReturnUrl("test_sierra_transformer")
  override val flags: Map[String, String] = Map(
    "aws.region" -> "eu-west-1",
    "aws.sqs.queue.url" -> queueUrl,
    "aws.sqs.waitTime" -> "1",
    "aws.sns.topic.arn" -> idMinterTopicArn,
    "aws.metrics.namespace" -> "sierra-transformer",
    "aws.s3.bucketName" -> bucketName
  )

  it("should transform sierra records, and publish them to the given topic") {

    val id = "b001"
    val title = "A pot of possums"
    val lastModifiedDate = Instant.now()

    val sierraHybridRecordMessage =
      hybridRecordSqsMessage(createValidSierraTransformableJson(
                               id,
                               title,
                               lastModifiedDate
                             ),
                             "sierra")

    sqsClient.sendMessage(queueUrl,
                          JsonUtil.toJson(sierraHybridRecordMessage).get)

    eventually {
      val snsMessages = listMessagesReceivedFromSNS()
      snsMessages should have size 1

      val sourceIdentifier = SourceIdentifier(
        IdentifierSchemes.sierraSystemNumber,
        id
      )

      val actualWork = JsonUtil.fromJson[UnidentifiedWork](snsMessages.head.message).get

      actualWork.sourceIdentifier shouldBe sourceIdentifier
      actualWork.title shouldBe Some(title)
      actualWork.identifiers shouldBe List(sourceIdentifier)
    }
  }

}
