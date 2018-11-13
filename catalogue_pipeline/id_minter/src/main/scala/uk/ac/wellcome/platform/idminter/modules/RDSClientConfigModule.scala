package uk.ac.wellcome.platform.idminter.modules

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.platform.idminter.config.models.RDSClientConfig

object RDSClientConfigModule extends TwitterModule {

  private val host =
    flag[String]("aws.rds.host", "", "Host of the MySQL database")
  private val port =
    flag[String]("aws.rds.port", "3306", "Port of the MySQL database")
  private val username = flag[String](
    "aws.rds.userName",
    "",
    "User to connect to the MySQL database")
  private val password = flag[String](
    "aws.rds.password",
    "",
    "Password to connect to the MySQL database")

  @Provides
  def providesRdsClientConfig(): RDSClientConfig =
    RDSClientConfig(
      host = host(),
      port = port(),
      username = username(),
      password = password()
    )
}
