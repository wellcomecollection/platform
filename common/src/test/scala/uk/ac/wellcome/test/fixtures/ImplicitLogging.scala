package uk.ac.wellcome.test.fixtures

import com.twitter.inject.Logging

trait ImplicitLogging extends Logging {

  implicit val implicitLogger = logger

}
