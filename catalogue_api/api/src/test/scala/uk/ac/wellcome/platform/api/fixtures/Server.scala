package uk.ac.wellcome.platform.api.fixtures

import org.scalatest.Suite
import uk.ac.wellcome.platform.api.{Server => AppServer}
import uk.ac.wellcome.test.fixtures.ServerFixtures

trait Server extends ServerFixtures { this: Suite =>
  def newAppServer: () => AppServer = () => new AppServer()

  val defaultFlags: Map[String, String] = Map()
}
