package uk.ac.wellcome.platform.recorder

import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.vhs.EmptyMetadata

import scala.concurrent.ExecutionContext.Implicits.global

class RecorderFeatureTest
    extends FunSpec
    with Matchers
    with IntegrationPatience
    with fixtures.Server
    with LocalVersionedHybridStore
    with Messaging
    with WorksGenerators {

  it("receives a transformed Work, and saves it to the VHS") {
    val work = createUnidentifiedWork

    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          withLocalSnsTopic { topic =>
            withTypeVHS[TransformedBaseWork, EmptyMetadata, Assertion](
              bucket = bucket,
              table = table) { _ =>
              val flags = sqsLocalClientFlags ++ vhsLocalFlags(bucket, table) ++ messageReaderLocalFlags(
                bucket,
                queue) ++ snsLocalFlags(topic)
              withServer(flags) { _ =>
                sendMessage[TransformedBaseWork](
                  bucket = bucket,
                  queue = queue,
                  obj = work)

                eventually {
                  assertStored[TransformedBaseWork](
                    bucket,
                    table,
                    id = work.sourceIdentifier.toString,
                    record = work)
                }
              }
            }
          }
        }
      }
    }
  }
}
