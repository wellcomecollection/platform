package uk.ac.wellcome.utils
import JsonUtil._
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.test.utils.JsonTestUtil

class JsonUtilTest extends FunSpec with Matchers with JsonTestUtil {
  case class A(id: String, b: B)
  case class B(id: String, c: C)
  case class C(ints: List[Int])

  describe("fromJson") {
    it("successfully parses a json string into an instance of a case class") {
      val aId = "a"
      val bId = "b"

      val inputString =
        s"""
           |{
           | "id": "$aId",
           | "b": {
           |   "id": "$bId",
           |   "c": {
           |     "ints": [1,2,3]
           |   }
           | }
           |}
        """.stripMargin

      val triedA = fromJson[A](inputString)
      triedA.isSuccess shouldBe true
      triedA.get shouldBe A(aId, B(bId, C(List(1, 2, 3))))
    }

    it("fails with GracefulFailureException if the json is invalid") {
      val triedA = fromJson[A]("not a valid json string")

      triedA.isFailure shouldBe true
      triedA.failed.get shouldBe a[GracefulFailureException]
    }

    it(
      "fails with GracefulFailureException if the json does not match the structure of the case class") {
      val triedA = fromJson[A]("""{"something": "else"}""")

      triedA.isFailure shouldBe true
      triedA.failed.get shouldBe a[GracefulFailureException]
    }

  }

  describe("toJson") {
    it("returns the json string representation of a case class") {
      val a = A(id = "A", b = B(id = "B", c = C(ints = List(1, 2, 3))))

      val triedString = toJson(a)
      triedString.isSuccess shouldBe true
      val expectedString =
        s"""
          |{
          | "id": "${a.id}",
          | "b": {
          |   "id": "${a.b.id}",
          |   "c": {
          |     "ints": [1,2,3]
          |   }
          | }
          |}
        """.stripMargin

      assertJsonStringsAreEqual(triedString.get, expectedString)
    }
  }

}
