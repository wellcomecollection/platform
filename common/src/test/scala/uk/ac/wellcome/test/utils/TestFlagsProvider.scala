package uk.ac.wellcome.test.utils

import org.scalatest.Suite


trait TestFlagsProvider { this: Suite =>
  def testFlags: Map[String, String] = Map()
}
