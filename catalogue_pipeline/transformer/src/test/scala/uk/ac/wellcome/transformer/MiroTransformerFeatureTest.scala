package uk.ac.wellcome.transformer

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.test.fixtures.{Messaging, S3, SNS, SQS}
import uk.ac.wellcome.messaging.test.fixtures.{MessageInfo, SNS, SQS}
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.transformer.transformers.MiroTransformableWrapper
import uk.ac.wellcome.transformer.utils.TransformableMessageUtils
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

class MiroTransformerFeatureTest
    extends FunSpec
      with Matchers
      with SQS
      with SNS
      with S3
      with Messaging
      with fixtures.Server
      with Eventually
      with ExtendedPatience
      with MiroTransformableWrapper
      with TransformableMessageUtils {

  it("transforms miro records and publishes the result to the given topic") {
    val miroID = "M0000001"
    val title = "A guide for a giraffe"

    withLocalSnsTopic { topicArn =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { bucket =>
          val miroHybridRecordMessage =
            hybridRecordSqsMessage(
              message = createValidMiroTransformableJson(
                MiroID = miroID,
                MiroCollection = "foo",
                data = s"""{
                  "image_title": "$title",
                  "image_cleared": "N",
                  "image_use_restrictions": "CC-0",
                  "image_copyright_cleared": "N",
                  "image_tech_file_size": ["100000"]
                }"""
              ),
              sourceName = "miro",
              s3Client = s3Client,
              bucket = bucket
            )

          sqsClient.sendMessage(
            queue.url,
            JsonUtil.toJson(miroHybridRecordMessage).get
          )

          val flags: Map[String, String] = Map(
            "aws.metrics.namespace" -> "sierra-transformer"
          ) ++ s3LocalFlags(bucket) ++ snsLocalFlags(topicArn) ++ sqsLocalFlags(
            queue)

          withServer(flags) { _ =>
            eventually {
              val snsMessages = listMessagesReceivedFromSNS(topicArn)
              snsMessages.length shouldBe >=(1)

              snsMessages.map { snsMessage =>
                val actualWork = get[UnidentifiedWork](snsMessage)

                actualWork.identifiers.head.value shouldBe miroID
                actualWork.title shouldBe Some(title)
              }
            }
          }
        }
      }
    }
  }
}
