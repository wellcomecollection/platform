package uk.ac.wellcome.transformer

import java.time.Instant

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.transformer.utils.{TransformableSQSMessageUtils, TransformerFeatureTest}

class SierraTransformerFeatureTest
  extends FunSpec
    with Matchers
    with TransformerFeatureTest
    with TransformableSQSMessageUtils {

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

    val id = "001"
    val title = "A pot of possums"
    val lastModifiedDate = "some string"

    createValidSierraBibSQSMessage(id, title, lastModifiedDate)



    eventually {
      false shouldBe true
    }
  }


}
