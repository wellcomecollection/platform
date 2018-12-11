package uk.ac.wellcome.platform.transformer.miro.services

import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal.{
  TransformedBaseWork,
  UnidentifiedWork
}
import uk.ac.wellcome.platform.transformer.miro.exceptions.MiroTransformerException
import uk.ac.wellcome.platform.transformer.miro.fixtures.MiroVHSRecordReceiverFixture
import uk.ac.wellcome.platform.transformer.miro.generators.MiroRecordGenerators
import uk.ac.wellcome.platform.transformer.miro.models.MiroMetadata
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord
import uk.ac.wellcome.storage.fixtures.S3

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

class MiroVHSRecordReceiverTest
    extends FunSpec
    with Matchers
    with SQS
    with SNS
    with S3
    with Messaging
    with Eventually
    with MiroVHSRecordReceiverFixture
    with IntegrationPatience
    with MockitoSugar
    with ScalaFutures
    with MiroRecordGenerators
    with WorksGenerators {

  case class TestException(message: String) extends Exception(message)

  def transformToWork(miroRecord: MiroRecord,
                      metadata: MiroMetadata,
                      version: Int) =
    Try(createUnidentifiedWorkWith(version = version))

  def failingTransformToWork(miroRecord: MiroRecord,
                             metadata: MiroMetadata,
                             version: Int) =
    Try(throw TestException("BOOOM!"))

  it("receives a message and sends it to SNS client") {
    withLocalSnsTopic { topic =>
      withLocalS3Bucket { bucket =>
        val message =
          createMiroVHSRecordNotificationMessageWith(bucket = bucket)

        withMiroVHSRecordReceiver(topic, bucket) { recordReceiver =>
          val future = recordReceiver.receiveMessage(message, transformToWork)

          whenReady(future) { _ =>
            val works = getMessages[TransformedBaseWork](topic)
            works.size should be >= 1

            works.map { work =>
              work shouldBe a[UnidentifiedWork]
            }
          }
        }
      }
    }
  }

  it("receives a message and adds the version to the transformed work") {
    val version = 5

    withLocalSnsTopic { topic =>
      withLocalS3Bucket { bucket =>
        val message = createMiroVHSRecordNotificationMessageWith(
          version = version,
          bucket = bucket
        )

        withMiroVHSRecordReceiver(topic, bucket) { recordReceiver =>
          val future = recordReceiver.receiveMessage(message, transformToWork)

          whenReady(future) { _ =>
            val works = getMessages[TransformedBaseWork](topic)
            works.size should be >= 1

            works.map { actualWork =>
              actualWork shouldBe a[UnidentifiedWork]
              val unidentifiedWork = actualWork.asInstanceOf[UnidentifiedWork]
              unidentifiedWork.version shouldBe version
            }
          }
        }
      }
    }
  }

  it("returns a failed future if there's no MiroMetadata") {
    withLocalSnsTopic { topic =>
      withLocalS3Bucket { bucket =>
        val incompleteMessage = createHybridRecordNotificationWith(
          createMiroRecord,
          s3Client = s3Client,
          bucket = bucket
        )

        withMiroVHSRecordReceiver(topic, bucket) { recordReceiver =>
          val future =
            recordReceiver.receiveMessage(incompleteMessage, transformToWork)

          whenReady(future.failed) {
            _ shouldBe a[MiroTransformerException]
          }
        }
      }
    }
  }

  it("returns a failed future if there's no HybridRecord") {
    withLocalSnsTopic { topic =>
      withLocalS3Bucket { bucket =>
        val incompleteMessage = createNotificationMessageWith(
          message = MiroMetadata(isClearedForCatalogueAPI = false)
        )

        withMiroVHSRecordReceiver(topic, bucket) { recordReceiver =>
          val future =
            recordReceiver.receiveMessage(incompleteMessage, transformToWork)

          whenReady(future.failed) {
            _ shouldBe a[MiroTransformerException]
          }
        }
      }
    }
  }

  it("fails if it's unable to perform a transformation") {
    withLocalSnsTopic { topic =>
      withLocalS3Bucket { bucket =>
        val message =
          createMiroVHSRecordNotificationMessageWith(bucket = bucket)

        withMiroVHSRecordReceiver(topic, bucket) { recordReceiver =>
          val future =
            recordReceiver.receiveMessage(message, failingTransformToWork)

          whenReady(future.failed) { x =>
            x shouldBe a[TestException]
          }
        }
      }
    }
  }

  it("fails if it's unable to publish the work") {
    withLocalS3Bucket { bucket =>
      val message = createMiroVHSRecordNotificationMessageWith(bucket = bucket)

      withMiroVHSRecordReceiver(Topic("no-such-topic"), bucket) {
        recordReceiver =>
          val future = recordReceiver.receiveMessage(message, transformToWork)

          whenReady(future.failed) {
            _.getMessage should include("Unknown topic: no-such-topic")
          }
      }
    }
  }
}
