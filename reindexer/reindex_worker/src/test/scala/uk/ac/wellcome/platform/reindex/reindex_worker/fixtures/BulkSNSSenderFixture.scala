package uk.ac.wellcome.platform.reindex.reindex_worker.fixtures

import uk.ac.wellcome.messaging.fixtures.SNS
import uk.ac.wellcome.platform.reindex.reindex_worker.services.BulkSNSSender
import uk.ac.wellcome.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

trait BulkSNSSenderFixture extends SNS {
  def withBulkSNSSender[R](testWith: TestWith[BulkSNSSender, R]): R =
    withSNSMessageWriter { snsMessageWriter =>
      val bulkSNSSender = new BulkSNSSender(
        snsMessageWriter = snsMessageWriter
      )
      testWith(bulkSNSSender)
    }
}
