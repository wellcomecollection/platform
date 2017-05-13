package uk.ac.wellcome.test.utils

import org.scalatest.concurrent.AbstractPatienceConfiguration
import org.scalatest.time.{Millis, Seconds, Span}

trait ExtendedPatience extends AbstractPatienceConfiguration {
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(15, Seconds)),
    interval = scaled(Span(150, Millis))
  )
}
