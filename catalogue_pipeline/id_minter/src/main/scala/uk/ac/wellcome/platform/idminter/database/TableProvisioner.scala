package uk.ac.wellcome.platform.idminter.database

import com.google.inject.Inject
import org.flywaydb.core.Flyway
import uk.ac.wellcome.platform.idminter.models.RDSClientConfig

import scala.collection.JavaConverters._

class TableProvisioner @Inject()(rdsClientConfig: RDSClientConfig) {

  def provision(database: String, tableName: String): Unit = {
    val flyway = new Flyway()
    flyway.setDataSource(
      s"jdbc:mysql://${rdsClientConfig.host}:${rdsClientConfig.port}/$database",
      rdsClientConfig.username,
      rdsClientConfig.password
    )
    flyway.setPlaceholders(
      Map("database" -> database, "tableName" -> tableName).asJava)
    flyway.migrate()
  }

}
