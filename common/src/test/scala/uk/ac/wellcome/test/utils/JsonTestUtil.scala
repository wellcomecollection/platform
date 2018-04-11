package uk.ac.wellcome.test.utils

import org.scalatest.Matchers
import io.circe.parser._

trait JsonTestUtil extends Matchers {

  def assertJsonStringsAreEqual(json1: String, json2: String) = {
    val tree1 = parse(json1).right.get
    val tree2 = parse(json2).right.get
    tree1 shouldBe tree2
  }

  def assertJsonStringsAreDifferent(json1: String, json2: String) = {
    val tree1 = parse(json1).right.get
    val tree2 = parse(json2).right.get
    tree1 shouldNot be(tree2)
  }
}
