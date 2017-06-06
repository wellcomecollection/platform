package uk.ac.wellcome.platform.idminter.utils

import org.scalatest.{BeforeAndAfterEach, Suite}
import scalikejdbc._
import uk.ac.wellcome.platform.idminter.model.IdentifiersTable

trait MysqlLocal extends BeforeAndAfterEach { this: Suite =>
  private val host = "localhost"
  private val port = "3306"
  private val userName = "root"
  private val password = "password"

  Class.forName("com.mysql.jdbc.Driver")
  ConnectionPool.singleton(s"jdbc:mysql://$host:$port", userName, password)
  implicit val session = AutoSession

  private val identifiersDatabase = SQLSyntax.createUnsafely("identifiers")
  private val identifiersTableName = SQLSyntax.createUnsafely("Identifiers")

  val identifiersTable =
    new IdentifiersTable(identifiersDatabase, identifiersTableName)
  DB localTx { implicit session =>
    sql"DROP DATABASE IF EXISTS $identifiersDatabase".execute().apply()
    sql"CREATE DATABASE $identifiersDatabase;".execute().apply()
    sql"""CREATE TABLE $identifiersDatabase.$identifiersTableName (
       CanonicalID varchar(255),
       MiroID varchar(255),
       PRIMARY KEY (CanonicalID),
       CONSTRAINT Unique_miro UNIQUE (MiroID));"""
      .execute()
      .apply()
  }

  val mySqlLocalEndpointFlags: Map[String, String] =
    Map(
      "aws.rds.host" -> host,
      "aws.rds.port" -> port,
      "aws.rds.userName" -> userName,
      "aws.rds.password" -> password,
      "aws.rds.identifiers.database" -> identifiersDatabase,
      "aws.rds.identifiers.table" -> identifiersTableName
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    sql"TRUNCATE TABLE $identifiersDatabase.$identifiersTableName".execute().apply()
  }
}
