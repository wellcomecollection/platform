package uk.ac.wellcome.platform.registrar

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.test.utils.ExtendedPatience

class RegistrarFeatureTest
  extends FunSpec
    with Matchers
    with ScalaFutures
    with fixtures.Registrar
    with MetricsSenderFixture
    with ExtendedPatience {

  it("runs") {
    withRegistrar {
      case (_,_,_, registrar) =>
        registrar.run()


    }
  }
}
