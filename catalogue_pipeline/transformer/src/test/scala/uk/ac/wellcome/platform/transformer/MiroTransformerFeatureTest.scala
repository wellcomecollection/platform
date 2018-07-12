package uk.ac.wellcome.platform.transformer

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.platform.transformer.transformers.MiroTransformableWrapper
import uk.ac.wellcome.platform.transformer.utils.TransformableMessageUtils
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

    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withLocalS3Bucket { messageBucket =>
            val miroHybridRecordMessage =
              hybridRecordNotificationMessage(
                message = createValidMiroTransformableJson(
                  MiroID = miroID,
                  MiroCollection = "foo",
                  data = buildJSONForWork(s""""image_title": "$title"""")
                ),
                sourceName = "miro",
                s3Client = s3Client,
                bucket = storageBucket
              )

            sqsClient.sendMessage(
              queue.url,
              JsonUtil.toJson(miroHybridRecordMessage).get
            )

            val flags: Map[String, String] = Map(
              "aws.metrics.namespace" -> "sierra-transformer"
            ) ++ s3LocalFlags(storageBucket) ++
              sqsLocalFlags(queue) ++ messageWriterLocalFlags(
              messageBucket,
              topic)

            withServer(flags) { _ =>
              eventually {
                val works = getMessages[UnidentifiedWork](topic)
                works.length shouldBe >=(1)

                works.map { actualWork =>
                  actualWork.identifiers.head.value shouldBe miroID
                  actualWork.title shouldBe title
                }
              }
            }
          }
        }
      }
    }
  }
}
