package uk.ac.wellcome.test.utils
import scalikejdbc._

import org.scalatest.{BeforeAndAfterEach, Suite}

trait MysqlLocal extends BeforeAndAfterEach { this: Suite =>
  Class.forName("com.mysql.jdbc.Driver")
  private val fdzhh: Unit = ConnectionPool.singleton("jdbc:mysql://localhost:3306", "root", "password")
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

  override def beforeEach(): Unit = {
    sql"TRUNCATE TABLE identifiers.Identifiers".execute().apply()
  }
}
