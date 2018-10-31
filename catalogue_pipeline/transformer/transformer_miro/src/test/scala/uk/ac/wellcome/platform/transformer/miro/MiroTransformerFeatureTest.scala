package uk.ac.wellcome.platform.transformer.miro

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.platform.transformer.miro.transformers.MiroTransformableWrapper
import uk.ac.wellcome.platform.transformer.miro.generators.MiroTransformableMessageGenerators
import uk.ac.wellcome.storage.fixtures.S3

class MiroTransformerFeatureTest
    extends FunSpec
    with Matchers
    with SQS
    with SNS
    with S3
    with Messaging
    with uk.ac.wellcome.platform.transformer.miro.fixtures.Server
    with Eventually
    with IntegrationPatience
    with MiroTransformableWrapper
    with MiroTransformableMessageGenerators {

  it("transforms miro records and publishes the result to the given topic") {
    val miroID = "M0000001"
    val title = "A guide for a giraffe"

    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withLocalS3Bucket { messageBucket =>
            val miroHybridRecordMessage = createHybridRecordNotificationWith(
              createValidMiroTransformableWith(
                miroId = miroID,
                miroCollection = "foo",
                data = buildJSONForWork(s""""image_title": "$title"""")
              ),
              s3Client = s3Client,
              bucket = storageBucket
            )

            sendSqsMessage(
              queue = queue,
              obj = miroHybridRecordMessage
            )

            val flags: Map[String, String] = Map(
              "aws.metrics.namespace" -> "miro-transformer"
            ) ++ s3ClientLocalFlags ++
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

  // This is based on a specific bug that we found where different records
  // were written to the same s3 key because of the hashing algorithm clashing
  it("sends different messages for different miro records") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withLocalS3Bucket { messageBucket =>
            val flags: Map[String, String] = Map(
              "aws.metrics.namespace" -> "miro-transformer"
            ) ++ s3ClientLocalFlags ++
              sqsLocalFlags(queue) ++ messageWriterLocalFlags(
              messageBucket,
              topic)

            withServer(flags) { _ =>
              val miroHybridRecordMessage1 = createHybridRecordNotificationWith(
                createValidMiroTransformableWith(
                  miroId = "L0011975",
                  miroCollection = "images-L",
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
                createValidMiroTransformableWith(
                  miroId = "L0023034",
                  miroCollection = "images-L",
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
}
