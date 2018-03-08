package uk.ac.wellcome.platform.idminter.fixtures

import scalikejdbc.{AutoSession, ConnectionPool, DB, SQLSyntax}
import uk.ac.wellcome.test.fixtures.TestWith
import scalikejdbc._

import scala.util.Random

case class DatabaseConfig(
  databaseName: String,
  tableName: String
)

trait IdentifiersDatabase {
  val host = "localhost"
  val port = "3307"
  val username = "root"
  val password = "password"

  Class.forName("com.mysql.jdbc.Driver")
  ConnectionPool.singleton(s"jdbc:mysql://$host:$port", username, password)
  implicit val session = AutoSession

  val mySqlLocalEndpointFlags: Map[String, String] =
    Map("aws.rds.host" -> host,
      "aws.rds.port" -> port,
      "aws.rds.userName" -> username,
      "aws.rds.password" -> password
    )


  def withIdentifiersDatabase[R](testWith: TestWith[DatabaseConfig, R]) = {
    val databaseName: String = Random.alphanumeric take 10 mkString
    val tableName: String = Random.alphanumeric take 10 mkString

    val identifiersDatabase: SQLSyntax = SQLSyntax.createUnsafely(databaseName)
    val identifiersTableName: SQLSyntax = SQLSyntax.createUnsafely(tableName)

    try {
      sql"CREATE DATABASE $identifiersDatabase;".execute().apply()

      val flags = mySqlLocalEndpointFlags ++ Map(
        "aws.rds.identifiers.table" -> tableName,
        "aws.rds.identifiers.database" -> databaseName
      )

      val config = DatabaseConfig(
        databaseName = databaseName,
        tableName = tableName
      )

      testWith(config)

    } finally {
      DB localTx { implicit session =>
        sql"DROP DATABASE IF EXISTS $identifiersDatabase".execute().apply()
      }
    }
  }
}
