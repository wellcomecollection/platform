package uk.ac.wellcome.platform.idminter.utils

import org.scalatest.Suite
import org.scalatest.concurrent.Eventually
import scalikejdbc._
import uk.ac.wellcome.platform.idminter.models.IdentifiersTable
import uk.ac.wellcome.test.utils.ExtendedPatience

trait MysqlLocal extends ExtendedPatience with Eventually { this: Suite =>
  val host = "localhost"
  val port = "3307"
  val userName = "root"
  val password = "password"

  Class.forName("com.mysql.jdbc.Driver")
  ConnectionPool.singleton(s"jdbc:mysql://$host:$port", userName, password)
  implicit val session = AutoSession

  val identifiersDatabase: SQLSyntax = SQLSyntax.createUnsafely("identifiers")

  val mySqlLocalEndpointFlags: Map[String, String] =
    Map(
      "aws.rds.host" -> host,
      "aws.rds.port" -> port,
      "aws.rds.userName" -> userName,
      "aws.rds.password" -> password,
      "aws.rds.identifiers.database" -> identifiersDatabase
    )

  // Wait for mysql server to start
  eventually {
    DB localTx { implicit session =>
      sql"DROP DATABASE IF EXISTS $identifiersDatabase".execute().apply()
      sql"CREATE DATABASE $identifiersDatabase;".execute().apply()
    }
  }

}

trait IdentifiersTableInfo { this: MysqlLocal =>
  val identifiersTableName: SQLSyntax = SQLSyntax.createUnsafely("Identifiers")

  val identifiersTable =
    new IdentifiersTable(identifiersDatabase, identifiersTableName)

  val identifiersMySqlLocalFlags: Map[String, String] =
    mySqlLocalEndpointFlags + ("aws.rds.identifiers.table" -> identifiersTableName)
}
