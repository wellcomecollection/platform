package uk.ac.wellcome.platform.idminter.fixtures

import org.scalatest.Matchers
import org.scalatest.concurrent.Eventually
import scalikejdbc.{AutoSession, ConnectionPool, DB, SQLSyntax}
import uk.ac.wellcome.test.fixtures.TestWith
import scalikejdbc._
import uk.ac.wellcome.platform.idminter.database.FieldDescription
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.util.Random

case class DatabaseConfig(
  databaseName: String,
  tableName: String,
  database: SQLSyntax,
  table: SQLSyntax,
  flags: Map[String, String],
  session: AutoSession.type
)

trait IdentifiersDatabase
    extends Eventually
    with ExtendedPatience
    with Matchers {

  val host = "localhost"
  val port = "3307"
  val username = "root"
  val password = "password"

  def eventuallyTableExists(databaseConfig: DatabaseConfig) = eventually {
    val database = databaseConfig.database
    val table = databaseConfig.table

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

    fields.sortBy(_.field) shouldBe Seq(
      FieldDescription(
        field = "CanonicalId",
        dataType = "varchar(255)",
        nullable = "NO",
        key = "PRI"),
      FieldDescription(
        field = "OntologyType",
        dataType = "varchar(255)",
        nullable = "NO",
        key = "MUL"),
      FieldDescription(
        field = "SourceSystem",
        dataType = "varchar(255)",
        nullable = "NO",
        key = ""),
      FieldDescription(
        field = "SourceId",
        dataType = "varchar(255)",
        nullable = "NO",
        key = "")
    ).sortBy(_.field)
  }

  def withIdentifiersDatabase[R](testWith: TestWith[DatabaseConfig, R]) = {
    Class.forName("com.mysql.jdbc.Driver")
    ConnectionPool.singleton(s"jdbc:mysql://$host:$port", username, password)

    implicit val session = AutoSession

    val databaseName: String = Random.alphanumeric take 10 mkString
    val tableName: String = Random.alphanumeric take 10 mkString

    val identifiersDatabase: SQLSyntax = SQLSyntax.createUnsafely(databaseName)
    val identifiersTable: SQLSyntax = SQLSyntax.createUnsafely(tableName)

    val flags = Map(
      "aws.rds.host" -> host,
      "aws.rds.port" -> port,
      "aws.rds.userName" -> username,
      "aws.rds.password" -> password,
      "aws.rds.identifiers.table" -> tableName,
      "aws.rds.identifiers.database" -> databaseName
    )

    val config = DatabaseConfig(
      databaseName = databaseName,
      tableName = tableName,
      database = identifiersDatabase,
      table = identifiersTable,
      flags = flags,
      session = session
    )

    try {
      sql"CREATE DATABASE $identifiersDatabase".execute().apply()

      testWith(config)
    } finally {
      DB localTx { implicit session =>
        sql"DROP DATABASE IF EXISTS $identifiersDatabase".execute().apply()
      }

      session.close()
    }


  }
}
