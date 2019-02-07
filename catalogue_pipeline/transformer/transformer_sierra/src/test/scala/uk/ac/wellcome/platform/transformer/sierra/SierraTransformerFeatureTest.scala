package uk.ac.wellcome.platform.transformer.sierra

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.SierraTransformable._
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraGenerators
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.platform.transformer.sierra.fixtures.HybridRecordReceiverFixture
import uk.ac.wellcome.platform.transformer.sierra.services.SierraTransformerWorkerService
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.test.fixtures.Akka

import scala.concurrent.ExecutionContext.Implicits.global

class SierraTransformerFeatureTest
    extends FunSpec
    with Matchers
    with Akka
    with SQS
    with SNS
    with S3
    with Messaging
    with Eventually
    with HybridRecordReceiverFixture
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

            withWorkerService(topic, messagingBucket, queue) { _ =>
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

  def withWorkerService[R](topic: Topic, bucket: Bucket, queue: Queue)(
    testWith: TestWith[SierraTransformerWorkerService, R]): R =
    withHybridRecordReceiver[SierraTransformable, R](topic, bucket) {
      messageReceiver =>
        withActorSystem { implicit actorSystem =>
          withSQSStream[NotificationMessage, R](queue) { sqsStream =>
            val workerService = new SierraTransformerWorkerService(
              messageReceiver = messageReceiver,
              sierraTransformer = new SierraTransformableTransformer,
              sqsStream = sqsStream
            )

            workerService.run()

            testWith(workerService)
          }
        }
    }
}
