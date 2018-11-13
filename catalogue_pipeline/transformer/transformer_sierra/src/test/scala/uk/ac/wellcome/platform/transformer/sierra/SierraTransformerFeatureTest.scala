package uk.ac.wellcome.platform.transformer.sierra

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.SierraTransformable._
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraGenerators
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.platform.transformer.receive.HybridRecordReceiver
import uk.ac.wellcome.platform.transformer.sierra.services.SierraTransformerWorkerService
import uk.ac.wellcome.storage.fixtures.S3

class SierraTransformerFeatureTest
    extends FunSpec
    with Matchers
    with SQS
    with SNS
    with S3
    with Messaging
    with fixtures.Server
    with Eventually
    with IntegrationPatience
    with SierraGenerators {

  it("transforms sierra records and publishes the result to the given topic") {
    val id = createSierraBibNumber
    val title = "A pot of possums"

    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withLocalS3Bucket { messagingBucket =>
            val data =
              s"""
                 |{
                 | "id": "$id",
                 | "title": "$title",
                 | "varFields": []
                 |}
                  """.stripMargin

            val sierraTransformable = SierraTransformable(
              bibRecord = createSierraBibRecordWith(
                id = id,
                data = data
              )
            )

            val sierraHybridRecordMessage = createHybridRecordNotificationWith(
              sierraTransformable,
              s3Client = s3Client,
              bucket = storageBucket
            )

            sendSqsMessage(
              queue = queue,
              obj = sierraHybridRecordMessage
            )

            val workerService = new SierraTransformerWorkerService(
              messageReceiver = new HybridRecordReceiver[SierraTransformable](

              )
            )

            val flags: Map[String, String] = Map(
              "aws.metrics.namespace" -> "sierra-transformer"
            ) ++ s3ClientLocalFlags ++
              sqsLocalFlags(queue) ++ messageWriterLocalFlags(
              messagingBucket,
              topic)

            withServer(flags) { _ =>
              eventually {
                val snsMessages = listMessagesReceivedFromSNS(topic)
                snsMessages.size should be >= 1

                val sourceIdentifier = createSierraSystemSourceIdentifierWith(
                  value = id.withCheckDigit
                )

                val sierraIdentifier =
                  createSierraIdentifierSourceIdentifierWith(
                    value = id.withoutCheckDigit
                  )

                val works = getMessages[UnidentifiedWork](topic)
                works.length shouldBe >=(1)

                works.map { actualWork =>
                  actualWork.sourceIdentifier shouldBe sourceIdentifier
                  actualWork.title shouldBe title
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
}
