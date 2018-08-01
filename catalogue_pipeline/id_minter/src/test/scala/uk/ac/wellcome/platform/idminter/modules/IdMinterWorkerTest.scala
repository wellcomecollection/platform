package uk.ac.wellcome.platform.idminter.modules

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import scalikejdbc._
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.platform.idminter.database.{
  FieldDescription,
  IdentifiersDao
}
import uk.ac.wellcome.platform.idminter.fixtures
import uk.ac.wellcome.storage.fixtures.S3

class IdMinterWorkerTest
    extends FunSpec
    with SQS
    with SNS
    with S3
    with Messaging
    with fixtures.IdentifiersDatabase
    with fixtures.Server
    with Eventually
    with IntegrationPatience
    with Matchers
    with MockitoSugar {

  it("creates the Identifiers table in MySQL upon startup") {
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withIdentifiersDatabase { identifiersTableConfig =>
          withLocalS3Bucket { bucket =>
            val flags =
              messagingLocalFlags(bucket, topic, queue) ++ identifiersLocalDbFlags(
                identifiersTableConfig)

            val identifiersDao = mock[IdentifiersDao]

            withModifiedServer(
              flags,
              modifyServer = (server: EmbeddedHttpServer) => {
                server.bind[IdentifiersDao].toInstance(identifiersDao)
              }) { _ =>
              val database: SQLSyntax =
                SQLSyntax.createUnsafely(identifiersTableConfig.database)
              val table: SQLSyntax =
                SQLSyntax.createUnsafely(identifiersTableConfig.tableName)

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
