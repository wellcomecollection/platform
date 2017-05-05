package uk.ac.wellcome.platform.api

import org.scalatest.{FunSpec, Matchers}

import uk.ac.wellcome.models.Work
import uk.ac.wellcome.utils.JsonUtil


class WorkTest extends FunSpec with Matchers {
  it("should have an LD type 'Work' when serialised to JSON") {
    val work = Work(identifiers=List(), label="A book about a thing")
    val jsonString = JsonUtil.toJson(work).get

    jsonString.contains("""type":"Work"""") should be (true)
  }
}
