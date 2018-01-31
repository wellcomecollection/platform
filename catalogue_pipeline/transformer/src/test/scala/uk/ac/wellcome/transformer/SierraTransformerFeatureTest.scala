package uk.ac.wellcome.transformer

import java.time.Instant

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.models.{IdentifierSchemes, SourceIdentifier, Work}
import uk.ac.wellcome.transformer.utils.{
  TransformableSQSMessageUtils,
  TransformerFeatureTest
}
import uk.ac.wellcome.utils.JsonUtil

class SierraTransformerFeatureTest
    extends FunSpec
    with Matchers
    with TransformerFeatureTest
    with TransformableSQSMessageUtils {

  override lazy val bucketName: String = "test-sierra-transformer-feature-test-bucket"
  val queueUrl: String = createQueueAndReturnUrl("test_sierra_transformer")
  override val flags: Map[String, String] = Map(
    "transformer.source" -> "SierraData",
    "aws.region" -> "eu-west-1",
    "aws.sqs.queue.url" -> queueUrl,
    "aws.sqs.waitTime" -> "1",
    "aws.sns.topic.arn" -> idMinterTopicArn,
    "aws.metrics.namespace" -> "sierra-transformer"
  )

  it("should transform sierra records, and publish them to the given topic") {

    val id = "b001"
    val title = "A pot of possums"
    val lastModifiedDate = Instant.now()

    val calmSqsMessage = hybridRecordSqsMessage(
      createValidSierraTransformableJson(
        id,
        title,
        lastModifiedDate
      ))

    sqsClient.sendMessage(queueUrl, JsonUtil.toJson(calmSqsMessage).get)

    eventually {
      val snsMessages = listMessagesReceivedFromSNS()
      snsMessages should have size 1

      val sourceIdentifier = SourceIdentifier(
        IdentifierSchemes.sierraSystemNumber,
        id
      )

      val actualWork = JsonUtil.fromJson[Work](snsMessages.head.message).get

      actualWork.sourceIdentifier shouldBe sourceIdentifier
      actualWork.title shouldBe Some(title)
      actualWork.identifiers shouldBe List(sourceIdentifier)
    }
  }

}
