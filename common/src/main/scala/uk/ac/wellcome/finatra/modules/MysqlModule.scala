package uk.ac.wellcome.finatra.modules

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import scalikejdbc.{AutoSession, ConnectionPool, DB}

object MysqlModule extends TwitterModule {

  val host = flag[String]("aws.rds.host", "", "Host of the MySQL database")
  val port = flag[String]("aws.rds.port", "3306", "Port of the MySQL database")
  val userName = flag[String]("aws.rds.userName", "", "User to connect to the MySQL database")
  val password = flag[String]("aws.rds.password", "", "Password to connect to the MySQL database")

  @Provides
  def providesDB(): DB = {
    Class.forName("com.mysql.jdbc.Driver")
    ConnectionPool.singleton(s"jdbc:mysql://${host()}:${port()}", userName(), password())
    implicit val session = AutoSession
    DB.connect()
  }
}
