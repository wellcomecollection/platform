package uk.ac.wellcome.platform.idminter.utils

import org.scalatest.{BeforeAndAfterEach, Suite}
import scalikejdbc._
import uk.ac.wellcome.platform.idminter.database.TableProvisioner

trait IdentifiersMysqlLocal
    extends MysqlLocal
    with BeforeAndAfterEach
    with IdentifiersTableInfo { this: Suite =>

  new TableProvisioner(host, port, userName, password)
    .provision(identifiersDatabase, identifiersTableName)

  override def beforeEach(): Unit = {
    super.beforeEach()
    sql"TRUNCATE TABLE $identifiersDatabase.$identifiersTableName"
      .execute()
      .apply()
  }
}
