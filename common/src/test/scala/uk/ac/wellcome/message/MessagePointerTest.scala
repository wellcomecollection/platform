package uk.ac.wellcome.message

import org.scalatest._
import uk.ac.wellcome.utils.JsonUtil._
import java.net.URI
import scala.util.Success

class MessagePointerTest extends FunSpec with Matchers {

  it("reads from json") {
    val pointer = fromJson[MessagePointer]("""{"src":"http://example.org"}""")
    pointer shouldBe Success(MessagePointer("http://example.org"))
  }

  it("writes to json") {
    val pointer = MessagePointer(new URI("http://example.org"))
    toJson(pointer) shouldBe Success("""{"src":"http://example.org"}""")
  }

}
