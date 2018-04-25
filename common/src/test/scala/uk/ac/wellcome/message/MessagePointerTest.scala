package uk.ac.wellcome.message

import org.scalatest._
import uk.ac.wellcome.utils.JsonUtil._
import java.net.URI

import uk.ac.wellcome.s3.S3ObjectLocation

import scala.util.Success

class MessagePointerTest extends FunSpec with Matchers {

  it("reads from json") {
    val pointer = fromJson[MessagePointer]("""{"src":{"bucket":"bucket","key":"key"}}""")
    val messagePointer = MessagePointer.create(new URI("s3://bucket/key")).get

    pointer shouldBe Success(messagePointer)
  }

  it("writes to json") {
    val pointer = MessagePointer.create(new URI("s3://bucket/key")).get

    toJson(pointer) shouldBe Success("""{"src":{"bucket":"bucket","key":"key"}}""")
  }

}
