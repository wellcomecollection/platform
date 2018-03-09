package uk.ac.wellcome.platform.idminter.modules

import java.sql.SQLSyntaxErrorException

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import scalikejdbc._
import uk.ac.wellcome.platform.idminter.database.{
  FieldDescription,
  IdentifiersDao
}
import uk.ac.wellcome.platform.idminter.{fixtures, Server}
import uk.ac.wellcome.test.fixtures.{SnsFixtures, SqsFixtures}
import uk.ac.wellcome.test.utils.ExtendedPatience

class IdMinterWorkerTest
    extends FunSpec
    with SqsFixtures
    with SnsFixtures
    with fixtures.IdentifiersDatabase
    with fixtures.Server
    with Eventually
    with ExtendedPatience
    with Matchers
    with MockitoSugar {

  it("should create the Identifiers table in MySQL upon startup") {
    withLocalSqsQueue { queueUrl =>
      withLocalSnsTopic { topicArn =>
        withIdentifiersDatabase { dbConfig =>
          val flags = Map(
            "aws.region" -> "localhost",
            "aws.sqs.queue.url" -> queueUrl,
            "aws.sqs.waitTime" -> "1",
            "aws.sns.topic.arn" -> topicArn
          ) ++ sqsLocalFlags ++ snsLocalFlags ++ dbConfig.flags

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
