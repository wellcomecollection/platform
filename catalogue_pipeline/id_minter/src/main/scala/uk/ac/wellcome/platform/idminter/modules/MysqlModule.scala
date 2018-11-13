package uk.ac.wellcome.platform.idminter.modules

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import scalikejdbc.{ConnectionPool, ConnectionPoolSettings, DB}
import uk.ac.wellcome.platform.idminter.config.models.RDSClientConfig

object MysqlModule extends TwitterModule {
  override val modules = Seq(RDSClientConfigModule)
  val maxSize = flag[Int](
    "aws.rds.maxConnections",
    "Maximum number of connections to the database")

  @Provides
  def providesDB(rdsClientConfig: RDSClientConfig): DB = {
    Class.forName("com.mysql.jdbc.Driver")
    ConnectionPool.singleton(
      s"jdbc:mysql://${rdsClientConfig.host}:${rdsClientConfig.port}",
      user = rdsClientConfig.username,
      password = rdsClientConfig.password,
      settings = ConnectionPoolSettings(maxSize = maxSize())
    )
    DB.connect()
  }
}
