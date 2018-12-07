package uk.ac.wellcome.platform.recorder

import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.Messaging
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.recorder.fixtures.WorkerServiceFixture
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore

class RecorderFeatureTest
    extends FunSpec
    with Matchers
    with IntegrationPatience
    with LocalVersionedHybridStore
    with Messaging
    with WorkerServiceFixture
    with WorksGenerators {

  it("receives a transformed Work, and saves it to the VHS") {
    val work = createUnidentifiedWork

    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          withLocalSnsTopic { topic =>
            withWorkerService(table, bucket, topic, queue) { _ =>
              sendMessage[TransformedBaseWork](queue = queue, obj = work)

              eventually {
                assertStored[TransformedBaseWork](
                  table = table,
                  id = work.sourceIdentifier.toString,
                  record = work
                )
              }
            }
          }
        }
      }
    }
  }
}
