package uk.ac.wellcome.platform.archive.common.progress.fixtures

import java.time.Instant
import java.time.format.DateTimeFormatter

import com.gu.scanamo.DynamoFormat
import uk.ac.wellcome.platform.archive.common.progress.monitor.ArchiveProgressMonitor
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.test.fixtures.TestWith

trait ArchiveProgressMonitorFixtures extends LocalProgressMonitorDynamoDb {

  implicit val instantLongFormat: AnyRef with DynamoFormat[Instant] =
    DynamoFormat.coercedXmap[Instant, String, IllegalArgumentException](str =>
      Instant.from(DateTimeFormatter.ISO_INSTANT.parse(str)))(
      DateTimeFormatter.ISO_INSTANT.format(_)
    )

  def withArchiveProgressMonitor[R](table: Table)(
    testWith: TestWith[ArchiveProgressMonitor, R]): R = {
    val archiveProgressMonitor = new ArchiveProgressMonitor(
      dynamoDbClient,
      DynamoConfig(table = table.name, index = table.index)
    )
    testWith(archiveProgressMonitor)
  }
}
