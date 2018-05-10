package uk.ac.wellcome.platform.idminter.modules

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import scalikejdbc.{ConnectionPool, DB}

object MysqlModule extends TwitterModule {

  private val host =
    flag[String]("aws.rds.host", "", "Host of the MySQL database")
  private val port =
    flag[String]("aws.rds.port", "3306", "Port of the MySQL database")
  private val userName = flag[String](
    "aws.rds.userName",
    "",
    "User to connect to the MySQL database")
  private val password = flag[String](
    "aws.rds.password",
    "",
    "Password to connect to the MySQL database")

  @Provides
  def providesDB(): DB = {
    Class.forName("com.mysql.jdbc.Driver")
    ConnectionPool.singleton(
      s"jdbc:mysql://${host()}:${port()}",
      userName(),
      password())
    DB.connect()
  }
}
