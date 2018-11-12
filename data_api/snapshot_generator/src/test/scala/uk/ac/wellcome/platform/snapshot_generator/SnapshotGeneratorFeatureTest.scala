package uk.ac.wellcome.platform.snapshot_generator

import java.io.File

import com.amazonaws.services.s3.model.GetObjectRequest
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.display.models.ApiVersions
import uk.ac.wellcome.display.models.v1.DisplayV1SerialisationTestBase
import uk.ac.wellcome.elasticsearch.DisplayElasticConfig
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.monitoring.fixtures.CloudWatch
import uk.ac.wellcome.platform.snapshot_generator.fixtures.AkkaS3
import uk.ac.wellcome.platform.snapshot_generator.models.{
  CompletedSnapshotJob,
  SnapshotJob
}
import uk.ac.wellcome.platform.snapshot_generator.test.utils.GzipUtils
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.utils.JsonAssertions
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.platform.snapshot_generator.services.{SnapshotGeneratorWorkerService, SnapshotService}

import scala.concurrent.ExecutionContext.Implicits.global

class SnapshotGeneratorFeatureTest
    extends FunSpec
    with Eventually
    with Matchers
    with Akka
    with AkkaS3
    with S3
    with SNS
    with SQS
    with CloudWatch
    with GzipUtils
    with JsonAssertions
    with IntegrationPatience
    with ElasticsearchFixtures
    with DisplayV1SerialisationTestBase
    with WorksGenerators {

  val itemType = "work"

  it("completes a snapshot generation") {
    withFixtures {
      case (queue, topic, indexNameV1, _, publicBucket: Bucket) =>
        val works = createIdentifiedWorks(count = 3)

        insertIntoElasticsearch(indexNameV1, itemType, works: _*)

        val publicObjectKey = "target.txt.gz"

        val snapshotJob = SnapshotJob(
          publicBucketName = publicBucket.name,
          publicObjectKey = publicObjectKey,
          apiVersion = ApiVersions.v1
        )

        sendNotificationToSQS(queue = queue, message = snapshotJob)

        eventually {
          val downloadFile =
            File.createTempFile("snapshotGeneratorFeatureTest", ".txt.gz")

          s3Client.getObject(
            new GetObjectRequest(publicBucket.name, publicObjectKey),
            downloadFile)

          val actualJsonLines: List[String] =
            readGzipFile(downloadFile.getPath).split("\n").toList

          val expectedJsonLines = works.sortBy { _.canonicalId }.map { work =>
            s"""{
                 |  "id": "${work.canonicalId}",
                 |  "title": "${work.title}",
                 |  "identifiers": [ ${identifier(work.sourceIdentifier)} ],
                 |  "creators": [ ],
                 |  "genres": [ ],
                 |  "subjects": [ ],
                 |  "items": [ ],
                 |  "publishers": [ ],
                 |  "placesOfPublication": [ ],
                 |  "type": "Work"
                   }""".stripMargin
          }

          actualJsonLines.sorted.zip(expectedJsonLines).foreach {
            case (actualLine, expectedLine) =>
              assertJsonStringsAreEqual(actualLine, expectedLine)
          }

          val receivedMessages = listMessagesReceivedFromSNS(topic)
          receivedMessages.size should be >= 1

          val expectedJob = CompletedSnapshotJob(
            snapshotJob = snapshotJob,
            targetLocation =
              s"http://localhost:33333/${publicBucket.name}/$publicObjectKey"
          )
          val actualJob =
            fromJson[CompletedSnapshotJob](receivedMessages.head.message).get
          actualJob shouldBe expectedJob
        }
    }

  }

  def withFixtures[R](
    testWith: TestWith[(Queue, Topic, String, String, Bucket), R]): R =
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withLocalElasticsearchIndex(itemType = itemType) { indexV1name =>
          withLocalElasticsearchIndex(itemType = itemType) { indexV2name =>
            withLocalS3Bucket { bucket =>
              withActorSystem { actorSystem =>
                withMaterializer(actorSystem) { materializer =>
                  withS3AkkaClient(actorSystem, materializer) { akkaS3Client =>
                    val snapshotService = new SnapshotService(
                      actorSystem = actorSystem,
                      akkaS3Client = akkaS3Client,
                      elasticClient = elasticClient,
                      elasticConfig = DisplayElasticConfig(
                        documentType = itemType,
                        indexV1name = indexV1name,
                        indexV2name = indexV2name
                      )
                    )

                    withSQSStream[NotificationMessage, R](actorSystem, queue) { sqsStream =>
                      withSNSWriter(topic) { snsWriter =>
                        val service = new SnapshotGeneratorWorkerService(
                          snapshotService = snapshotService,
                          sqsStream = sqsStream,
                          snsWriter = snsWriter
                        )

                        service.run()

                        testWith((queue, topic, indexV1name, indexV2name, bucket))
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

}
