package uk.ac.wellcome.platform.archive.progress_http

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.platform.archive.progress_http.fixtures.ProgressHttpFixture
import uk.ac.wellcome.test.utils.ExtendedPatience

class ProgressHttpFeatureTest
  extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with ProgressMonitorFixture
    with ProgressHttpFixture
    with ExtendedPatience {

  it("creates a progress record") {
    withConfiguredApp {
      case (table, _, app) => {
        app.run()

        assertProgressCreated(
          "id",
          uploadUrl,
          Some(callbackUrl),
          table = table
        )

      }
    }
  }
}

