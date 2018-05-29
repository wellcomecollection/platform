package uk.ac.wellcome.platform.recorder

import io.circe.Decoder
import io.circe.generic.extras.semiauto.deriveDecoder
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.{
  IdentifierType,
  SourceIdentifier,
  UnidentifiedWork
}
import uk.ac.wellcome.storage.s3.S3ObjectLocation
import uk.ac.wellcome.storage.test.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.vhs.EmptyMetadata
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

class RecorderFeatureTest
    extends FunSpec
    with Matchers
    with ExtendedPatience
    with fixtures.Server
    with LocalVersionedHybridStore
    with Messaging {

  implicit val decoder: Decoder[UnidentifiedWork] =
    deriveDecoder[UnidentifiedWork]

  it("receives a transformed Work, and saves it to the VHS") {
    val title = "Not from Guildford after all"

    val sourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("MiroImageNumber"),
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
          withTypeVHS[RecorderWorkEntry, EmptyMetadata, Assertion](
            bucket = bucket,
            table = table) { _ =>
            val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(bucket, table) ++ messageReaderLocalFlags(
              bucket,
              queue)
            withServer(flags) { _ =>
              val messageBody = put[UnidentifiedWork](
                obj = work,
                location = S3ObjectLocation(
                  bucket = bucket.name,
                  key = "work_message.json"
                )
              )

              sqsClient.sendMessage(queue.url, messageBody)

              eventually {
                assertStored[RecorderWorkEntry](
                  bucket,
                  table,
                  RecorderWorkEntry(work))
              }
            }
          }
        }
      }
    }
  }

}
