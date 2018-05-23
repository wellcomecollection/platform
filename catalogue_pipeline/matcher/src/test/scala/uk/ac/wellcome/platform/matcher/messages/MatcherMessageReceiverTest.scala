package uk.ac.wellcome.platform.matcher.messages

import com.amazonaws.services.s3.AmazonS3
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig, SNSWriter}
import uk.ac.wellcome.messaging.sqs.{SQSConfig, SQSStream}
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.matcher.fixtures.{LocalLinkedWorkDynamoDb, MatcherFixtures}
import uk.ac.wellcome.platform.matcher.matcher.LinkedWorkMatcher
import uk.ac.wellcome.platform.matcher.models.{IdentifierList, LinkedWorksIdentifiersList}
import uk.ac.wellcome.platform.matcher.storage.{LinkedWorkDao, MatcherDynamoConfig, WorkGraphStore}
import uk.ac.wellcome.storage.s3.{S3Config, S3TypeStore}
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.HybridRecord
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.duration._

class MatcherMessageReceiverTest
    extends FunSpec
    with Matchers
    with Akka
    with SQS
    with SNS
    with S3
    with MetricsSenderFixture
    with ExtendedPatience
    with MatcherFixtures
    with Eventually
      with LocalLinkedWorkDynamoDb {

  def withLinkedWorkMatcher[R](table: Table)(testWith: TestWith[LinkedWorkMatcher, R]): R = {
    val workGraphStore = new WorkGraphStore(new LinkedWorkDao(dynamoDbClient, MatcherDynamoConfig(table.name, table.index)))
    val linkedWorkMatcher = new LinkedWorkMatcher(workGraphStore)
    testWith(linkedWorkMatcher)
  }
  def withMatcherMessageReceiver[R](
    queue: SQS.Queue,
    storageBucket: Bucket,
    topic: Topic)(testWith: TestWith[MatcherMessageReceiver, R]) = {
    val storageS3Config = S3Config(storageBucket.name)

    val snsWriter =
      new SNSWriter(snsClient, SNSConfig(topic.arn))

    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withLocalDynamoDbTable { table =>
          withLinkedWorkMatcher(table) { linkedWorkMatcher =>
            implicit val executionContext = actorSystem.dispatcher
            val sqsStream = new SQSStream[NotificationMessage](
              actorSystem = actorSystem,
              sqsClient = asyncSqsClient,
              sqsConfig = SQSConfig(queue.url, 1 second, 1),
              metricsSender = metricsSender
            )
            val matcherMessageReceiver = new MatcherMessageReceiver(
              sqsStream,
              snsWriter,
              new S3TypeStore[RecorderWorkEntry](s3Client),
              storageS3Config,
              actorSystem,
              linkedWorkMatcher)
            testWith(matcherMessageReceiver)
          }
        }
      }
    }
  }

  it("sends no redirects for a work without identifiers") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          sendSQS(queue, storageBucket, anUnidentifiedSierraWork)

          withMatcherMessageReceiver(queue, storageBucket, topic) { _ =>
            eventually {
              assertMessageSent(
                topic,
                LinkedWorksIdentifiersList(
                  Set(IdentifierList(Set("sierra-system-number/id"))))
              )
            }
          }
        }
      }
    }
  }

  it(
    "work A with one link to B and no existing works returns a single matched work") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          val linkedIdentifier = aSierraSourceIdentifier("B")
          val aIdentifier = aSierraSourceIdentifier("A")
          val work = anUnidentifiedSierraWork.copy(
            sourceIdentifier = aIdentifier,
            identifiers = List(aIdentifier, linkedIdentifier))

          sendSQS(queue, storageBucket, work)

          withMatcherMessageReceiver(queue, storageBucket, topic) { _ =>
            eventually {
              assertMessageSent(
                topic,
                LinkedWorksIdentifiersList(Set(IdentifierList(
                  Set("sierra-system-number/A", "sierra-system-number/B"))))
              )
            }
          }
        }
      }
    }
  }

  it("redirects a work with one link and existing redirects") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withMatcherMessageReceiver(queue, storageBucket, topic) { _ =>
            val aIdentifier = aSierraSourceIdentifier("A")
            val bIdentifier = aSierraSourceIdentifier("B")
            val cIdentifier = aSierraSourceIdentifier("C")

            val aWork = anUnidentifiedSierraWork.copy(
              sourceIdentifier = aIdentifier,
              identifiers = List(aIdentifier, bIdentifier))

            sendSQS(queue, storageBucket, aWork)

            eventually {

              assertMessageSent(
                topic,
                LinkedWorksIdentifiersList(
                  Set(
                    IdentifierList(
                      Set(
                        "sierra-system-number/A",
                        "sierra-system-number/B"
                      ))))
              )

              val bWork = anUnidentifiedSierraWork.copy(
                sourceIdentifier = bIdentifier,
                identifiers = List(bIdentifier, cIdentifier))

              sendSQS(queue, storageBucket, bWork)

              eventually {

                assertMessageSent(
                  topic,
                  LinkedWorksIdentifiersList(
                    Set(
                      IdentifierList(
                        Set(
                          "sierra-system-number/A",
                          "sierra-system-number/B",
                          "sierra-system-number/C"
                        ))))
                )
              }
            }
          }
        }
      }
    }
  }

  it("breaks a set of works into individual works") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withMatcherMessageReceiver(queue, storageBucket, topic) { _ =>
            val aIdentifier = aSierraSourceIdentifier("A")
            val bIdentifier = aSierraSourceIdentifier("B")

            val aWork = anUnidentifiedSierraWork.copy(
              sourceIdentifier = aIdentifier,
              identifiers = List(aIdentifier, bIdentifier))

            sendSQS(queue, storageBucket, aWork)

            eventually {

              assertMessageSent(
                topic,
                LinkedWorksIdentifiersList(
                  Set(
                    IdentifierList(
                      Set(
                        "sierra-system-number/A",
                        "sierra-system-number/B"
                      ))))
              )

              val aWorkWithoutLinks = anUnidentifiedSierraWork.copy(
                sourceIdentifier = aIdentifier,
                identifiers = List(aIdentifier))

              sendSQS(queue, storageBucket, aWorkWithoutLinks)

              eventually {

                assertMessageSent(
                  topic,
                  LinkedWorksIdentifiersList(
                    Set(
                      IdentifierList(
                        Set(
                          "sierra-system-number/A"
                        )),
                      IdentifierList(
                        Set(
                          "sierra-system-number/B"
                        ))
                    ))
                )
              }
            }
          }
        }
      }
    }
  }

  private def assertMessageSent(
    topic: Topic,
    identifiersList: LinkedWorksIdentifiersList) = {
    val snsMessages = listMessagesReceivedFromSNS(topic)
    snsMessages.size should be >= 1

    val actualMatchedWorkLists = snsMessages.map { snsMessage =>
      fromJson[LinkedWorksIdentifiersList](snsMessage.message).get
    }
    actualMatchedWorkLists should contain(identifiersList)
  }

  private def sendSQS(queue: SQS.Queue,
                      storageBucket: Bucket,
                      work: UnidentifiedWork) = {
    val workSqsMessage: NotificationMessage =
      hybridRecordNotificationMessage(
        message = toJson(RecorderWorkEntry(work = work)).get,
        version = 1,
        s3Client = s3Client,
        bucket = storageBucket
      )
    sqsClient.sendMessage(
      queue.url,
      toJson(workSqsMessage).get
    )
  }

  def hybridRecordNotificationMessage(message: String,
                                      version: Int,
                                      s3Client: AmazonS3,
                                      bucket: Bucket) = {
    val key = "recorder/1/testId/dshg548.json"
    s3Client.putObject(bucket.name, key, message)

    val hybridRecord = HybridRecord(
      id = "testId",
      version = version,
      s3key = key
    )

    NotificationMessage(
      "messageId",
      "topicArn",
      "subject",
      toJson(hybridRecord).get
    )
  }

}
