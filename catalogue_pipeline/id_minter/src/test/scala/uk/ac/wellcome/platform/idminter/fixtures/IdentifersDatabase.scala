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

  // This is based on the implementation of alphanumeric in Scala.util.Random.
  def alphabetic: Stream[Char] = {
    def nextAlpha: Char = {
      val chars = "abcdefghijklmnopqrstuvwxyz"
      chars charAt (Random.nextInt(chars.length))
    }

    Stream continually nextAlpha
  }

  def withIdentifiersDatabase[R](testWith: TestWith[DatabaseConfig, R]) = {
    Class.forName("com.mysql.jdbc.Driver")
    ConnectionPool.singleton(s"jdbc:mysql://$host:$port", username, password)

    implicit val session = AutoSession

    // Something in our MySQL Docker image gets upset by some database names,
    // and throws an error of the form:
    //
    //    You have an error in your SQL syntax; check the manual that
    //    corresponds to your MySQL server version for the right syntax to use
    //
    // This error can be reproduced by running "CREATE DATABASE <name>" inside
    // the Docker image.  It's not clear what features of the name cause this
    // error to occur, but so far we've only seen it in database names that
    // include numbers.  We're guessing some arrangement of numbers causes the
    // issue; for now we just use letters to try to work around this issue.
    //
    // The Oracle docs are not enlightening in this regard:
    // https://docs.oracle.com/database/121/SQLRF/sql_elements008.htm#SQLRF00223
    val databaseName: String = alphabetic take 10 mkString
    val tableName: String = alphabetic take 10 mkString

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
