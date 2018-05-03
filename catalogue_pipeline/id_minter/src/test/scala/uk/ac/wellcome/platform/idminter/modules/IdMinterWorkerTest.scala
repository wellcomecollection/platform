package uk.ac.wellcome.platform.idminter.modules

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import scalikejdbc._
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.platform.idminter.database.{
  FieldDescription,
  IdentifiersDao
}
import uk.ac.wellcome.platform.idminter.fixtures
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.test.utils.ExtendedPatience

class IdMinterWorkerTest
    extends FunSpec
    with SQS
    with SNS
    with S3
    with Messaging
    with fixtures.IdentifiersDatabase
    with fixtures.Server
    with Eventually
    with ExtendedPatience
    with Matchers
    with MockitoSugar {

  it("should create the Identifiers table in MySQL upon startup") {
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withIdentifiersDatabase { dbConfig =>
          withLocalS3Bucket { bucket =>
            val flags =
              sqsLocalFlags(queue) ++ snsLocalFlags(topic) ++
                messagingLocalFlags(bucket, topic) ++ dbConfig.flags

            val identifiersDao = mock[IdentifiersDao]

            withServer(flags, (server: EmbeddedHttpServer) => {
              server.bind[IdentifiersDao](identifiersDao)
            }) { _ =>
              val database = dbConfig.database
              val table = dbConfig.table

              eventually {
                val fields = DB readOnly { implicit session =>
                  sql"DESCRIBE $database.$table"
                    .map(
                      rs =>
                        FieldDescription(
                          rs.string("Field"),
                          rs.string("Type"),
                          rs.string("Null"),
                          rs.string("Key")))
                    .list()
                    .apply()
                }

                fields.length should be > 0
              }
            }
          }
        }
      }
    }
  }
}
