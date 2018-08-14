package uk.ac.wellcome.platform.archive.registrar

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.test.utils.ExtendedPatience

import uk.ac.wellcome.platform.archive.registrar.fixtures.{ Registrar => RegistrarFixture }

class RegistrarFeatureTest
  extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with ExtendedPatience
    with RegistrarFixture  {

  it("runs") {
    withRegistrar {
      case (_,_,_,_,registrar) =>
        registrar.run()


    }
  }
}
