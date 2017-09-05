package uk.ac.wellcome.test.utils

import com.fasterxml.jackson.databind.ObjectMapper
import org.scalatest.Matchers

trait JsonTestUtil { this: Matchers =>
  val mapper = new ObjectMapper()

  def assertJsonStringsAreEqual(json1: String, json2: String) = {
    val tree1 = mapper.readTree(json1)
    val tree2 = mapper.readTree(json2)
    tree1 shouldBe tree2
  }

  def assertJsonStringsAreDifferent(json1: String, json2: String) = {
    val tree1 = mapper.readTree(json1)
    val tree2 = mapper.readTree(json2)
    tree1 shouldNot be (tree2)
  }
}
