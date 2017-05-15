package uk.ac.wellcome.test.utils

import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time.{Millis, Seconds, Span}

trait ExtendedPatience extends PatienceConfiguration {
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(15, Seconds)),
    interval = scaled(Span(150, Millis))
  )
}
