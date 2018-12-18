package uk.ac.wellcome.platform.idminter.services

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import scalikejdbc._
import uk.ac.wellcome.messaging.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.platform.idminter.database.{
  FieldDescription,
  IdentifiersDao
}
import uk.ac.wellcome.platform.idminter.fixtures
import uk.ac.wellcome.platform.idminter.fixtures.WorkerServiceFixture
import uk.ac.wellcome.storage.fixtures.S3

class IdMinterWorkerServiceTest
    extends FunSpec
    with SQS
    with SNS
    with S3
    with Messaging
    with fixtures.IdentifiersDatabase
    with Eventually
    with IntegrationPatience
    with Matchers
    with MockitoSugar
    with WorkerServiceFixture {

  it("creates the Identifiers table in MySQL upon startup") {
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withIdentifiersDatabase { identifiersTableConfig =>
          withLocalS3Bucket { bucket =>
            val identifiersDao = mock[IdentifiersDao]
            withWorkerService(
              bucket,
              topic,
              queue,
              identifiersDao,
              identifiersTableConfig) { _ =>
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
