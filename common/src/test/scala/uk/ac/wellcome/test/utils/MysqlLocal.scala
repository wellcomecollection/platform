package uk.ac.wellcome.test.utils
import scalikejdbc._

import org.scalatest.{BeforeAndAfterEach, Suite}

trait MysqlLocal extends BeforeAndAfterEach { this: Suite =>
  private val host = "localhost"
  private val port = "3306"
  private val userName = "root"
  private val password = "password"

  Class.forName("com.mysql.jdbc.Driver")
  ConnectionPool.singleton(s"jdbc:mysql://$host:$port", userName, password)
  implicit val session = AutoSession

  val identifiersTableName = "Identifiers"
  DB localTx { implicit session =>
    sql"DROP DATABASE IF EXISTS identifiers".execute().apply()
    sql"CREATE DATABASE identifiers;".execute().apply()
    sql"""CREATE TABLE identifiers.Identifiers (
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
      "aws.rds.password" -> password
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    sql"TRUNCATE TABLE identifiers.Identifiers".execute().apply()
  }
}
