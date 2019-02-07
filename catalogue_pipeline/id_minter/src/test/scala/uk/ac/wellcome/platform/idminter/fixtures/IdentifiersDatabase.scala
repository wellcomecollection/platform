package uk.ac.wellcome.platform.idminter.fixtures

import org.scalatest.{Assertion, Matchers}
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import scalikejdbc.{AutoSession, ConnectionPool, DB, SQLSyntax}
import uk.ac.wellcome.fixtures.TestWith
import scalikejdbc._
import uk.ac.wellcome.platform.idminter.config.models.{
  IdentifiersTableConfig,
  RDSClientConfig
}
import uk.ac.wellcome.platform.idminter.database.FieldDescription

import scala.util.Random

trait IdentifiersDatabase
    extends Eventually
    with IntegrationPatience
    with Matchers {

  val host = "localhost"
  val port = 3307
  val username = "root"
  val password = "password"

  def eventuallyTableExists(tableConfig: IdentifiersTableConfig): Assertion =
    eventually {
      val database: SQLSyntax = SQLSyntax.createUnsafely(tableConfig.database)
      val table: SQLSyntax = SQLSyntax.createUnsafely(tableConfig.tableName)

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

  val rdsClientConfig = RDSClientConfig(
    host = host,
    port = port,
    username = username,
    password = password
  )

  def withIdentifiersDatabase[R](
    testWith: TestWith[IdentifiersTableConfig, R]): R = {
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

    val identifiersTableConfig = IdentifiersTableConfig(
      database = databaseName,
      tableName = tableName
    )

    try {
      sql"CREATE DATABASE $identifiersDatabase".execute().apply()

      testWith(identifiersTableConfig)
    } finally {
      DB localTx { implicit session =>
        sql"DROP DATABASE IF EXISTS $identifiersDatabase".execute().apply()
      }

      session.close()
    }

  }
}
