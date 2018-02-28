package uk.ac.wellcome.platform.idminter.database

import com.google.inject.Inject
import com.twitter.inject.annotations.Flag
import org.flywaydb.core.Flyway

import scala.collection.JavaConversions._

class TableProvisioner @Inject()(@Flag("aws.rds.host") host: String,
                                 @Flag("aws.rds.port") port: String,
                                 @Flag("aws.rds.userName") userName: String,
                                 @Flag("aws.rds.password") password: String) {

  def provision(database: String, tableName: String): Unit = {
    val flyway = new Flyway()
    flyway.setDataSource(
      s"jdbc:mysql://$host:$port/$database",
      userName,
      password)
    flyway.setPlaceholders(
      Map("database" -> database, "tableName" -> tableName))
    flyway.migrate()
  }

}
