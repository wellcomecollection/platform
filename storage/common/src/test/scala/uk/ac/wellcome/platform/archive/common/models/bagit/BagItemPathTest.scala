package uk.ac.wellcome.platform.archive.common.models.bagit

import org.scalatest.{FunSpec, Matchers}

class BagItemPathTest extends FunSpec with Matchers {

  it("can be created") {
    BagItemPath("bag-info.txt").underlying shouldBe "bag-info.txt"
  }

  it("can be created with an optional root path") {
    BagItemPath(Some("bag"), "bag-info.txt").underlying shouldBe "bag/bag-info.txt"
  }

  it("normalises item path when joining paths") {
    BagItemPath(Some("bag"), "/bag-info.txt").underlying shouldBe "bag/bag-info.txt"
  }

  it("normalises root path when joining paths") {
    BagItemPath(Some("bag/"), "bag-info.txt").underlying shouldBe "bag/bag-info.txt"
  }

}
