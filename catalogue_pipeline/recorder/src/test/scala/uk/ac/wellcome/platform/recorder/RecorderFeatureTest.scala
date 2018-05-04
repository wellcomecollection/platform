package uk.ac.wellcome.platform.recorder

import io.circe.Encoder
import io.circe.generic.extras.semiauto.deriveEncoder
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SQS}
import uk.ac.wellcome.models.work.internal.{IdentifierSchemes, SourceIdentifier, UnidentifiedWork}
import uk.ac.wellcome.platform.recorder.models.RecorderWorkEntry
import uk.ac.wellcome.storage.s3.S3ObjectLocation
import uk.ac.wellcome.storage.test.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

class RecorderFeatureTest
    extends FunSpec
      with Matchers
      with ExtendedPatience
      with fixtures.Server
      with LocalVersionedHybridStore
      with Messaging
      with SQS {

  implicit val encoder: Encoder[UnidentifiedWork] = deriveEncoder[UnidentifiedWork]

  // TODO Write a test that older works are ignored

  it("receives a transformed Work, and saves it to the VHS") {
    val title = "Not from Guildford after all"

    val sourceIdentifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.miroImageNumber,
      value = "V0237865",
      ontologyType = "Work"
    )

    val work = UnidentifiedWork(
      title = Some(title),
      sourceIdentifier = sourceIdentifier,
      identifiers = List(sourceIdentifier),
      version = 1
    )

    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          withVersionedHybridStore[RecorderWorkEntry, Unit](bucket = bucket, table = table) { _ =>

            val flags = sqsLocalFlags(queue) ++ s3LocalFlags(bucket) ++ dynamoDbLocalEndpointFlags(table)
            withServer(flags) { _ =>
              val messageBody = put[UnidentifiedWork](
                obj = work,
                location = S3ObjectLocation(
                  bucket = bucket.name, key = "work_message.json"
                )
              )

              sqsClient.sendMessage(queue.url, messageBody)

              eventually {
                assertStored[RecorderWorkEntry](bucket, table, RecorderWorkEntry(work))
              }
            }
          }
        }
      }
    }
  }

}
