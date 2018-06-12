package uk.ac.wellcome.platform.recorder

import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.{
  IdentifierType,
  SourceIdentifier,
  UnidentifiedWork
}
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.vhs.EmptyMetadata
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

class RecorderFeatureTest
    extends FunSpec
    with Matchers
    with ExtendedPatience
    with fixtures.Server
    with LocalVersionedHybridStore
    with Messaging {

  it("receives a transformed Work, and saves it to the VHS") {
    val title = "Not from Guildford after all"

    val sourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("miro-image-number"),
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
                location = ObjectLocation(
                  namespace = bucket.name,
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
