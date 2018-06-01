package uk.ac.wellcome.platform.idminter.modules

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import scalikejdbc.{ConnectionPool, DB}
import uk.ac.wellcome.platform.idminter.models.RDSClientConfig

object MysqlModule extends TwitterModule {
  override val modules = Seq(RDSClientConfigModule)

  @Provides
  def providesDB(rdsClientConfig: RDSClientConfig): DB = {
    Class.forName("com.mysql.jdbc.Driver")
    ConnectionPool.singleton(
      s"jdbc:mysql://${rdsClientConfig.host}:${rdsClientConfig.port}",
      user = rdsClientConfig.username,
      password = rdsClientConfig.password
    )
    DB.connect()
  }
}
