package uk.ac.wellcome.platform.transformer.miro

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.platform.transformer.fixtures.HybridRecordReceiverFixture
import uk.ac.wellcome.platform.transformer.miro.transformers.MiroTransformableWrapper
import uk.ac.wellcome.platform.transformer.miro.generators.MiroTransformableGenerators
import uk.ac.wellcome.platform.transformer.miro.models.MiroTransformable
import uk.ac.wellcome.platform.transformer.miro.services.MiroTransformerWorkerService
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

class MiroTransformerFeatureTest
    extends FunSpec
    with Matchers
    with SQS
    with SNS
    with S3
    with Messaging
    with Eventually
    with HybridRecordReceiverFixture
    with IntegrationPatience
    with MiroTransformableWrapper
    with MiroTransformableGenerators {

  it("transforms miro records and publishes the result to the given topic") {
    val miroId = "M0000001"
    val title = "A guide for a giraffe"

    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withLocalS3Bucket { messageBucket =>
            val miroHybridRecordMessage = createHybridRecordNotificationWith(
              createMiroTransformableWith(
                miroId = miroId,
                data = buildJSONForWork(miroId = miroId, s""""image_title": "$title"""")
              ),
              s3Client = s3Client,
              bucket = storageBucket
            )

            sendSqsMessage(
              queue = queue,
              obj = miroHybridRecordMessage
            )

            withWorkerService(topic, messageBucket, queue) { _ =>
              eventually {
                val works = getMessages[UnidentifiedWork](topic)
                works.length shouldBe >=(1)

                works.map { actualWork =>
                  actualWork.identifiers.head.value shouldBe miroId
                  actualWork.title shouldBe title
                }
              }
            }
          }
        }
      }
    }
  }

  // This is based on a specific bug that we found where different records
  // were written to the same s3 key because of the hashing algorithm clashing
  it("sends different messages for different miro records") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          val miroHybridRecordMessage1 = createHybridRecordNotificationWith(
            createMiroTransformableWith(
              miroId = "L0011975",
              data = """
                  |{
                  |  "image_cleared": "Y",
                  |  "image_copyright_cleared": "Y",
                  |  "image_credit_line": "Wellcome Library, London",
                  |  "image_image_desc": "Antonio Dionisi",
                  |  "image_innopac_id": "12917175",
                  |  "image_library_dept": "General Collections",
                  |  "image_no_calc": "L0011975",
                  |  "image_phys_format": "Book",
                  |  "image_tech_file_size": [
                  |    "5247788"
                  |  ],
                  |  "image_title": "Antonio Dionisi",
                  |  "image_use_restrictions": "CC-BY"
                  |}
                """.stripMargin
            ),
            s3Client = s3Client,
            bucket = storageBucket
          )
          val miroHybridRecordMessage2 = createHybridRecordNotificationWith(
            createMiroTransformableWith(
              miroId = "L0023034",
              data =
                """
                  |{
                  |  "image_cleared": "Y",
                  |  "image_copyright_cleared": "Y",
                  |  "image_image_desc": "Use of the guillotine",
                  |  "image_innopac_id": "12074536",
                  |  "image_keywords": [
                  |    "Surgery"
                  |  ],
                  |  "image_library_dept": "General Collections",
                  |  "image_no_calc": "L0023034",
                  |  "image_tech_file_size": [
                  |    "5710662"
                  |  ],
                  |  "image_title": "Greenfield Sluder, Tonsillectomy..., use of guillotine",
                  |  "image_use_restrictions": "CC-BY"
                  |}
                """.stripMargin
            ),
            s3Client = s3Client,
            bucket = storageBucket
          )
          withLocalS3Bucket { messageBucket =>
            withWorkerService(topic, messageBucket, queue) { _ =>
              sendSqsMessage(queue = queue, obj = miroHybridRecordMessage1)
              sendSqsMessage(queue = queue, obj = miroHybridRecordMessage2)

              eventually {
                val works = getMessages[UnidentifiedWork](topic)
                works.distinct.length shouldBe 2
              }
            }
          }
        }
      }
    }
  }

  def withWorkerService[R](topic: Topic, bucket: Bucket, queue: Queue)(
    testWith: TestWith[MiroTransformerWorkerService, R]): R =
    withHybridRecordReceiver[MiroTransformable, R](topic, bucket) {
      messageReceiver =>
        withActorSystem { actorSystem =>
          withSQSStream[NotificationMessage, R](actorSystem, queue) {
            sqsStream =>
              val workerService = new MiroTransformerWorkerService(
                messageReceiver = messageReceiver,
                miroTransformer = new MiroTransformableTransformer,
                sqsStream = sqsStream
              )

              workerService.run()

              testWith(workerService)
          }
        }
    }
}
