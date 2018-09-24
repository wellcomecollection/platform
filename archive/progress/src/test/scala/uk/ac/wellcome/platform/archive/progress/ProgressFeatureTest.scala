package uk.ac.wellcome.platform.archive.progress

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.platform.archive.progress.fixtures.{Progress => ProgressFixture}
import uk.ac.wellcome.test.utils.ExtendedPatience

class ProgressFeatureTest
  extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with ProgressMonitorFixture
    with ProgressFixture
    with ExtendedPatience {

  it("fails") {
    withProgress {
      case (qPair, topic, table, app) => {
        app.run()

        eventually {
          true shouldBe false
        }
      }
    }
  }
}
