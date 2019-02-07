package uk.ac.wellcome.platform.reindex.reindex_worker.fixtures

import uk.ac.wellcome.platform.reindex.reindex_worker.services.RecordReader
import uk.ac.wellcome.fixtures.TestWith

trait RecordReaderFixture extends DynamoFixtures {
  def withRecordReader[R](testWith: TestWith[RecordReader, R]): R =
    withMaxRecordsScanner { maxRecordsScanner =>
      withParallelScanner { parallelScanner =>
        val reader = new RecordReader(
          maxRecordsScanner = maxRecordsScanner,
          parallelScanner = parallelScanner
        )

        testWith(reader)
      }
    }
}
